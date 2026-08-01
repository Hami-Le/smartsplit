package com.smartsplit.ocr.service;

import com.smartsplit.expense.entity.Category;
import com.smartsplit.expense.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReceiptTextParser {
    private static final long MIN_REASONABLE_AMOUNT = 1_000L;
    private static final long MAX_REASONABLE_AMOUNT = 5_000_000_000L;

    /*
     * Deliberately does not allow arbitrary whitespace between dot/comma groups.
     * The previous expression could join two independent values such as
     * "8.100 118.390" into one invalid amount: 8,100,118,390.
     */
    private static final Pattern GROUPED_MONEY_PATTERN = Pattern.compile(
            "(?<!\\d)([+-]?\\d{1,3}(?:[.,]\\d{3}){1,3})(?![\\d.,])"
    );
    private static final Pattern SPACED_MONEY_PATTERN = Pattern.compile(
            "(?<!\\d)([+-]?\\d{1,3}(?:\\s+\\d{3}){1,3})(?!\\d)"
    );
    private static final Pattern PLAIN_MONEY_PATTERN = Pattern.compile(
            "(?<!\\d)([+-]?\\d{4,10})(?!\\d)"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})(?!\\d)"
                    + "|(?<!\\d)(\\d{4})-(\\d{1,2})-(\\d{1,2})(?!\\d)"
    );
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
            "(?:\\bVND\\b|VNĐ|₫|Đ|\\bD\\b)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final List<String> STRONG_TOTAL_KEYWORDS = List.of(
            "tong thanh toan", "tong cong", "tong tien", "thanh tien", "phai tra",
            "so tien giao dich", "gia tri giao dich", "so tien thanh toan", "amount paid",
            "amount due", "grand total", "payment amount", "transaction amount", "total amount"
    );
    private static final List<String> TOTAL_KEYWORDS = List.of(
            "tong", "total", "amount", "thanh toan", "so tien", "paid", "payment"
    );
    private static final List<String> IDENTIFIER_KEYWORDS = List.of(
            "so tai khoan", "tai khoan", "account number", "account no", "stk", "card number",
            "ma giao dich", "transaction id", "reference", "trace", "ma tham chieu", "ma don",
            "order id", "invoice no", "so hoa don", "mst", "ma so thue", "tax code", "otp",
            "so du", "balance", "phone", "dien thoai", "hotline"
    );
    private static final List<String> MERCHANT_LABELS = List.of(
            "ten cua hang", "cua hang", "merchant name", "merchant", "store name", "shop name",
            "don vi chap nhan thanh toan", "nha cung cap", "doi tac", "nguoi nhan", "ben nhan",
            "receiver", "recipient", "chuyen den", "den tai khoan", "den"
    );
    private static final Set<String> MERCHANT_SKIP = Set.of(
            "hoa don", "invoice", "receipt", "bill", "dia chi", "address", "mst", "ma so thue",
            "tel", "phone", "ngay", "date", "gio", "time", "ban", "table", "thu ngan", "cashier",
            "cam on", "thank you", "chi tiet giao dich", "transaction detail", "giao dich thanh cong",
            "thanh toan thanh cong", "bien lai", "payment receipt"
    );
    private static final Set<String> PLATFORM_OR_POS_SKIP = Set.of(
            "momo", "timo", "zalopay", "vnpay", "viettel money", "shopeepay",
            "dantrisoft", "kiotviet", "sapo", "pos365", "cukcuk", "ipos", "misa"
    );

    private final CategoryRepository categoryRepository;

    public ReceiptTextParser(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ParsedReceipt parse(String rawText) {
        String safeText = rawText == null ? "" : rawText.trim();
        List<String> lines = safeText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        FieldResult<String> merchant = findMerchant(lines);
        FieldResult<Long> total = findTotal(lines);
        FieldResult<LocalDate> date = findDate(lines, safeText);
        FieldResult<Category> category = suggestCategory(safeText);

        double confidenceValue = merchant.confidence() * 0.25d
                + total.confidence() * 0.45d
                + date.confidence() * 0.20d
                + category.confidence() * 0.10d;
        if (total.value() == null) {
            confidenceValue = Math.min(confidenceValue, 0.55d);
        }
        BigDecimal confidence = BigDecimal.valueOf(Math.max(0.05d, Math.min(0.95d, confidenceValue)))
                .setScale(4, RoundingMode.HALF_UP);

        return new ParsedReceipt(
                merchant.value(),
                total.value(),
                date.value(),
                category.value(),
                confidence
        );
    }

    private FieldResult<String> findMerchant(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String normalized = normalize(line);
            for (String label : MERCHANT_LABELS) {
                int labelPosition = normalized.indexOf(label);
                if (labelPosition < 0) continue;

                String inlineValue = valueAfterLabel(line);
                if (isValidMerchant(inlineValue)) {
                    return new FieldResult<>(cleanMerchant(inlineValue), 0.92d);
                }
                if (index + 1 < lines.size() && isValidMerchant(lines.get(index + 1))) {
                    return new FieldResult<>(cleanMerchant(lines.get(index + 1)), 0.88d);
                }
            }
        }

        List<ScoredText> candidates = new ArrayList<>();
        int limit = Math.min(lines.size(), 18);
        for (int index = 0; index < limit; index++) {
            String line = lines.get(index);
            if (!isValidMerchant(line)) continue;
            String normalized = normalize(line);

            int score = 100 - index * 4;
            if (line.equals(line.toUpperCase(Locale.ROOT))) score += 12;
            if (line.length() >= 4 && line.length() <= 55) score += 10;
            if (PLATFORM_OR_POS_SKIP.stream().anyMatch(normalized::contains)) score -= 80;
            if (normalized.contains("ngan hang") || normalized.contains("bank")) score -= 55;
            if (normalized.matches(".*\\bcn\\s*\\d+\\b.*")) score -= 20;
            candidates.add(new ScoredText(cleanMerchant(line), score));
        }

        return candidates.stream()
                .max(Comparator.comparingInt(ScoredText::score))
                .filter(candidate -> candidate.score() >= 35)
                .map(candidate -> new FieldResult<>(candidate.value(), 0.58d))
                .orElseGet(() -> FieldResult.empty());
    }

    private FieldResult<Long> findTotal(List<String> lines) {
        List<MoneyCandidate> candidates = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String normalized = normalize(line);
            boolean strongKeyword = containsAny(normalized, STRONG_TOTAL_KEYWORDS);
            boolean totalKeyword = strongKeyword || containsAny(normalized, TOTAL_KEYWORDS);

            collectMoneyCandidates(candidates, line, index, 0, totalKeyword, strongKeyword);

            if (totalKeyword) {
                if (index + 1 < lines.size()) {
                    collectMoneyCandidates(candidates, lines.get(index + 1), index + 1, 1, true, strongKeyword);
                }
                if (index + 2 < lines.size()) {
                    collectMoneyCandidates(candidates, lines.get(index + 2), index + 2, 2, true, strongKeyword);
                }
            }
        }

        return candidates.stream()
                .filter(candidate -> candidate.amount() >= MIN_REASONABLE_AMOUNT)
                .filter(candidate -> candidate.amount() <= MAX_REASONABLE_AMOUNT)
                .max(Comparator.comparingInt(MoneyCandidate::score)
                        .thenComparingLong(MoneyCandidate::amount))
                .map(candidate -> new FieldResult<>(candidate.amount(), confidenceForMoneyScore(candidate.score())))
                .orElseGet(() -> FieldResult.empty());
    }

    private void collectMoneyCandidates(
            List<MoneyCandidate> target,
            String line,
            int lineIndex,
            int distanceFromKeyword,
            boolean totalContext,
            boolean strongContext
    ) {
        String normalized = normalize(line);
        boolean identifierContext = containsAny(normalized, IDENTIFIER_KEYWORDS);
        boolean hasCurrency = CURRENCY_PATTERN.matcher(line).find();
        Set<String> rawValues = new LinkedHashSet<>();
        collectMatches(rawValues, GROUPED_MONEY_PATTERN, line);
        if (totalContext || hasCurrency) {
            collectMatches(rawValues, SPACED_MONEY_PATTERN, line);
        }
        collectMatches(rawValues, PLAIN_MONEY_PATTERN, line);

        for (String rawValue : rawValues) {
            Long amount = parseMoney(rawValue);
            if (amount == null || amount < MIN_REASONABLE_AMOUNT || amount > MAX_REASONABLE_AMOUNT) continue;
            if (looksLikeIdentifier(rawValue, normalized, identifierContext, totalContext, hasCurrency)) continue;

            int score = 20;
            if (strongContext) score += 180;
            else if (totalContext) score += 120;
            score -= distanceFromKeyword * 25;
            if (hasCurrency) score += 45;
            if (rawValue.contains(".") || rawValue.contains(",") || rawValue.matches(".*\\s+.*")) score += 20;
            if (amount >= 10_000L) score += 8;
            if (identifierContext) score -= 180;
            if (rawValues.size() > 1 && totalContext) {
                // On a total block the largest independently recognized value is usually the payable amount.
                score += Math.min(20, rawValue.length());
            }
            target.add(new MoneyCandidate(amount, score, lineIndex, rawValue));
        }
    }

    private void collectMatches(Set<String> target, Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            target.add(matcher.group(1).trim());
        }
    }

    private boolean looksLikeIdentifier(
            String rawValue,
            String normalizedLine,
            boolean identifierContext,
            boolean totalContext,
            boolean hasCurrency
    ) {
        String digits = rawValue.replaceAll("[^0-9]", "");
        if (digits.length() > 10) return true;
        if (identifierContext && !totalContext && !hasCurrency) return true;
        if (!rawValue.contains(".") && !rawValue.contains(",") && !rawValue.matches(".*\\s+.*")) {
            if (digits.length() >= 9 && !totalContext && !hasCurrency) return true;
        }
        return normalizedLine.matches(".*\\b20\\d{2}[01]\\d[0-3]\\d.*") && !totalContext;
    }

    private Long parseMoney(String raw) {
        try {
            String cleaned = raw.trim().replaceFirst("^[+-]", "");
            String digits = cleaned.replaceAll("[^0-9]", "");
            return digits.isBlank() ? null : Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double confidenceForMoneyScore(int score) {
        if (score >= 220) return 0.96d;
        if (score >= 170) return 0.90d;
        if (score >= 120) return 0.82d;
        if (score >= 70) return 0.66d;
        return 0.45d;
    }

    private FieldResult<LocalDate> findDate(List<String> lines, String text) {
        for (int index = 0; index < lines.size(); index++) {
            String normalized = normalize(lines.get(index));
            if (normalized.contains("ngay") || normalized.contains("date")
                    || normalized.contains("thoi gian") || normalized.contains("transaction time")) {
                Optional<LocalDate> sameLine = parseDate(lines.get(index));
                if (sameLine.isPresent()) return new FieldResult<>(sameLine.get(), 0.92d);
                if (index + 1 < lines.size()) {
                    Optional<LocalDate> nextLine = parseDate(lines.get(index + 1));
                    if (nextLine.isPresent()) return new FieldResult<>(nextLine.get(), 0.87d);
                }
            }
        }
        return parseDate(text)
                .map(date -> new FieldResult<>(date, 0.75d))
                .orElseGet(() -> FieldResult.empty());
    }

    private Optional<LocalDate> parseDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                LocalDate date;
                if (matcher.group(1) != null) {
                    int day = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    int year = Integer.parseInt(matcher.group(3));
                    if (year < 100) year += 2000;
                    date = LocalDate.of(year, month, day);
                } else {
                    date = LocalDate.of(
                            Integer.parseInt(matcher.group(4)),
                            Integer.parseInt(matcher.group(5)),
                            Integer.parseInt(matcher.group(6))
                    );
                }
                if (isReasonable(date)) return Optional.of(date);
            } catch (DateTimeException | NumberFormatException ignored) {
                // Continue with the next candidate.
            }
        }
        return Optional.empty();
    }

    private boolean isReasonable(LocalDate date) {
        return !date.isBefore(LocalDate.now().minusYears(10))
                && !date.isAfter(LocalDate.now().plusDays(2));
    }

    private FieldResult<Category> suggestCategory(String text) {
        String normalized = normalize(text);
        Map<String, List<String>> keywords = Map.of(
                "Ăn uống", List.of("nha hang", "quan", "coffee", "cafe", "bbq", "food", "an uong", "tra sua", "banh", "highlands", "phuc long", "kfc", "lotteria", "pizza"),
                "Di chuyển", List.of("taxi", "grab", "be ", "xang", "fuel", "bus", "ve xe", "parking", "giu xe", "cao toc", "toll"),
                "Khách sạn", List.of("hotel", "hostel", "resort", "khach san", "phong", "booking", "homestay"),
                "Mua sắm", List.of("shop", "store", "mart", "supermarket", "sieu thi", "shopee", "lazada", "quan ao", "fashion"),
                "Giải trí", List.of("cinema", "movie", "game", "karaoke", "rap phim", "ve xem", "ticket", "bowling")
        );

        Map.Entry<String, Long> best = keywords.entrySet().stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        entry.getValue().stream().filter(normalized::contains).count()
                ))
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("Khác", 0L));

        Category category = categoryRepository.findAll().stream()
                .filter(item -> item.getName().equalsIgnoreCase(best.getKey()))
                .findFirst()
                .orElse(null);
        if (category == null) return FieldResult.empty();
        return new FieldResult<>(category, best.getValue() > 0 ? 0.78d : 0.35d);
    }

    private boolean isValidMerchant(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 180) return false;
        if (trimmed.matches(".*\\d{7,}.*")) return false;
        if (DATE_PATTERN.matcher(trimmed).find()) return false;
        if (GROUPED_MONEY_PATTERN.matcher(trimmed).find() && letterCount(trimmed) < 3) return false;
        String normalized = normalize(trimmed);
        if (normalized.isBlank()) return false;
        if (MERCHANT_SKIP.stream().anyMatch(normalized::contains)) return false;
        if (containsAny(normalized, STRONG_TOTAL_KEYWORDS) || containsAny(normalized, TOTAL_KEYWORDS)) return false;
        return letterCount(trimmed) >= 2;
    }

    private String valueAfterLabel(String line) {
        int colon = Math.max(line.indexOf(':'), line.indexOf('：'));
        if (colon >= 0 && colon + 1 < line.length()) {
            return line.substring(colon + 1).trim();
        }
        int dash = line.indexOf(" - ");
        if (dash >= 0 && dash + 3 < line.length()) {
            return line.substring(dash + 3).trim();
        }
        return "";
    }

    private String cleanMerchant(String value) {
        return value.trim()
                .replaceAll("^[\\s:：|_-]+", "")
                .replaceAll("[\\s|_-]+$", "")
                .replaceAll("\\s{2,}", " ");
    }

    private int letterCount(String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (Character.isLetter(value.charAt(index))) count++;
        }
        return count;
    }

    private boolean containsAny(String normalized, Iterable<String> values) {
        for (String value : values) {
            if (normalized.contains(value)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9%]+", " ")
                .trim();
    }

    private record FieldResult<T>(T value, double confidence) {
        private static <T> FieldResult<T> empty() {
            return new FieldResult<>(null, 0.0d);
        }
    }

    private record MoneyCandidate(long amount, int score, int lineIndex, String rawValue) {
    }

    private record ScoredText(String value, int score) {
    }
}
