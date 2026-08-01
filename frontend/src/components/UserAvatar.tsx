type UserAvatarProps = {
  fullName: string
  avatarUrl?: string | null
  size?: 'small' | 'medium' | 'large'
  className?: string
}

export function UserAvatar({
  fullName,
  avatarUrl,
  size = 'small',
  className = '',
}: UserAvatarProps) {
  const classes = ['user-avatar', `user-avatar-${size}`, className].filter(Boolean).join(' ')
  const initial = fullName.trim().charAt(0).toUpperCase() || '?'
  const [imageFailed, setImageFailed] = useState(false)

  useEffect(() => setImageFailed(false), [avatarUrl])

  return avatarUrl && !imageFailed ? (
    <img className={classes} src={avatarUrl} alt={`Ảnh đại diện của ${fullName}`} onError={() => setImageFailed(true)} />
  ) : (
    <span className={classes} aria-hidden="true">{initial}</span>
  )
}
import { useEffect, useState } from 'react'
