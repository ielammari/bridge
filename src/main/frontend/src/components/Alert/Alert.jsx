import './Alert.css';

/** Form level feedback, announced through role="alert". */
export default function Alert({ tone = 'error', children }) {
  return (
    <div className={`alert alert--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
