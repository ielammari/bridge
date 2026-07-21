import './Alert.css';

/**
 * Form-level feedback. Uses role="alert" so screen readers announce a failed
 * submission without the user having to hunt for what changed.
 */
export default function Alert({ tone = 'error', children }) {
  return (
    <div className={`alert alert--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
