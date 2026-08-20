import Icon from '../Icon/Icon.jsx';
import { PASSWORD_RULES } from '../../constants/password.js';
import './PasswordField.css';

/**
 * The conditions a password must meet, ticking as it is typed. Guidance rather
 * than validation, so it never turns red.
 */
export default function PasswordRules({ id, value, context }) {
  return (
    <ul className="password__rules" id={id}>
      {PASSWORD_RULES.map((rule) => {
        const met = value ? rule.test(value, context) : false;
        return (
          <li key={rule.id} className={`password__rule${met ? ' password__rule--met' : ''}`}>
            <Icon name={met ? 'check' : 'dot'} />
            <span>{rule.label}</span>
            <span className="visually-hidden">{met ? ' : rempli' : ' : à remplir'}</span>
          </li>
        );
      })}
    </ul>
  );
}
