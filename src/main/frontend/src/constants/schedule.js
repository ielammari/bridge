import { clockTime, longDate } from './format.js';

/**
 * Whether an interview may be recorded: its hour has come, or its offer does
 * not hold its evaluators to it. One with no date is never recordable.
 */
export function isDue({ waitForAppointment, appointmentDate, appointmentTime }) {
  if (!appointmentDate) return false;
  if (!waitForAppointment) return true;
  return new Date(`${appointmentDate}T${appointmentTime}`) <= new Date();
}

/** When an interview that has not happened yet becomes recordable. */
export const opensText = ({ appointmentDate, appointmentTime }) =>
  `Saisie possible à partir du ${longDate(appointmentDate)} à ${clockTime(appointmentTime)}.`;
