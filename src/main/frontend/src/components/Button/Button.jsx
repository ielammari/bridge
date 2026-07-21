import './Button.css';

/**
 * Primary actions are named after what they do. Only one primary per screen;
 * everything else is secondary or a plain link.
 */
export default function Button({
  variant = 'primary',
  type = 'button',
  loading = false,
  disabled = false,
  fullWidth = false,
  className = '',
  children,
  ...rest
}) {
  const classes = ['button', `button--${variant}`];
  if (fullWidth) classes.push('button--full');
  if (className) classes.push(className);

  return (
    <button
      type={type}
      className={classes.join(' ')}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <span className="button__spinner" aria-hidden="true" />}
      {children}
    </button>
  );
}
