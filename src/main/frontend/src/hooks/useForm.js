import { useState } from 'react';

const isEmpty = (value) =>
  value === null || value === undefined || (typeof value === 'string' && value.trim() === '');

/**
 * Form state with two validation timings:
 * emptiness is reported only after a submit attempt, so a form can be filled in
 * any order; a malformed value is reported as soon as the field is left.
 *
 * A rule is { label, required?, format? }. `format` runs only on a value that
 * has content, and returns a message or null.
 *
 * A submit may cover a subset of the keys, for a screen whose actions demand
 * different fields. Only submitted keys start reporting emptiness.
 */
export default function useForm(initialValues, rules) {
  const [values, setValues] = useState(initialValues);
  const [touched, setTouched] = useState({});
  const [scope, setScope] = useState(null); // null, 'all', or a list of keys

  const inScope = (key) => scope === 'all' || (Array.isArray(scope) && scope.includes(key));

  function setValue(key, value) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  function errorFor(key) {
    const rule = rules[key];
    if (!rule) return undefined;

    if (isEmpty(values[key])) {
      return inScope(key) && rule.required ? rule.required : undefined;
    }
    if (!touched[key] && !inScope(key)) return undefined;
    return rule.format?.(values[key], values) ?? undefined;
  }

  function collectErrors(keys) {
    const considered = keys ?? Object.keys(rules);
    const found = {};
    for (const key of considered) {
      const rule = rules[key];
      if (!rule) continue;
      if (isEmpty(values[key])) {
        if (rule.required) found[key] = rule.required;
      } else {
        const message = rule.format?.(values[key], values);
        if (message) found[key] = message;
      }
    }
    return found;
  }

  /** The errors for whatever was last submitted, for the summary at the top. */
  function currentErrors() {
    if (scope === null) return {};
    return collectErrors(scope === 'all' ? undefined : scope);
  }

  /** Optional fields left empty, listed by a confirmation before committing. */
  function emptyOptional(keys) {
    return (keys ?? Object.keys(rules))
      .filter((key) => rules[key] && !rules[key].required && rules[key].label && isEmpty(values[key]))
      .map((key) => ({ key, label: rules[key].label }));
  }

  /**
   * Turns emptiness errors on for the given keys (all by default), focuses the
   * first offender, and reports whether the form is clean. For a submit with a
   * step in between, such as a confirmation dialog.
   */
  function attempt(keys) {
    setScope(keys ?? 'all');
    const found = collectErrors(keys);
    const first = Object.keys(found)[0];
    if (first) {
      document.querySelector(`[name="${first}"]`)?.focus();
      return false;
    }
    return true;
  }

  function handleSubmit(onValid) {
    return async (event) => {
      event?.preventDefault?.();
      if (!attempt()) return;
      await onValid(values);
    };
  }

  // Everything a Field or Select needs for one key. `name` is what attempt()
  // targets when moving focus.
  function field(key) {
    return {
      name: key,
      value: values[key] ?? '',
      onChange: (event) => setValue(key, event.target.value),
      onBlur: () => setTouched((current) => ({ ...current, [key]: true })),
      error: errorFor(key),
      required: Boolean(rules[key]?.required),
    };
  }

  return {
    values,
    setValue,
    setValues,
    field,
    errorFor,
    collectErrors,
    currentErrors,
    emptyOptional,
    attempt,
    handleSubmit,
    isEmpty,
  };
}
