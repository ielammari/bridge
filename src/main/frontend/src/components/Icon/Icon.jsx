// Inline single stroke icons, sized by the surrounding font size (1em) and
// coloured by currentColor so they inherit from context.
const PATHS = {
  search: <circle cx="11" cy="11" r="7" />,
  chevron: <polyline points="6 9 12 15 18 9" />,
  check: <polyline points="20 6 9 17 4 12" />,
  close: <><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></>,
  download: <><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" /></>,
  file: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /></>,
  upload: <><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" /></>,
  bell: <><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" /></>,
  eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7" /><circle cx="12" cy="12" r="3" /></>,
  'eye-off': <><path d="M10.6 5.2A9.9 9.9 0 0 1 12 5c6.5 0 10 7 10 7a17.7 17.7 0 0 1-3.4 4.3" /><path d="M6.6 6.6A17.7 17.7 0 0 0 2 12s3.5 7 10 7a9.9 9.9 0 0 0 4.2-.9" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /><line x1="3" y1="3" x2="21" y2="21" /></>,
  bookmark: <path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.5L5 21V4a1 1 0 0 1 1-1z" />,
  info: <><line x1="12" y1="11" x2="12" y2="17" /><line x1="12" y1="7.5" x2="12" y2="7.5" /></>,
  warning: <><path d="M12 3.5 2.5 20h19z" /><line x1="12" y1="10" x2="12" y2="14" /><line x1="12" y1="17" x2="12" y2="17" /></>,
  dot: <circle cx="12" cy="12" r="3.5" />,
  retry: <><path d="M21 12a9 9 0 1 1-2.6-6.4" /><polyline points="21 3 21 9 15 9" /></>,
  briefcase: <><rect x="2" y="7" width="20" height="14" rx="2" /><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></>,
  stack: <><rect x="3" y="4" width="18" height="5" rx="1" /><rect x="3" y="12" width="18" height="5" rx="1" /><line x1="7" y1="20" x2="17" y2="20" /></>,
  star: <path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9 6.8 19.6l1-5.8L3.5 9.7l5.9-.9z" />,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
  settings: <><circle cx="12" cy="12" r="3" /><path d="M12 2v3M12 19v3M4.2 4.2l2.2 2.2M17.6 17.6l2.2 2.2M2 12h3M19 12h3M4.2 19.8l2.2-2.2M17.6 6.4l2.2-2.2" /></>,
  logout: <><path d="M14 21H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8" /><polyline points="17 16 21 12 17 8" /><line x1="21" y1="12" x2="9" y2="12" /></>,
  panel: <><rect x="3" y="4" width="18" height="16" rx="2" /><line x1="10" y1="4" x2="10" y2="20" /></>,
  menu: <><line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" /></>,
  archive: <><rect x="3" y="4" width="18" height="4" rx="1" /><path d="M5 8v11a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8" /><line x1="10" y1="13" x2="14" y2="13" /></>,
};

export default function Icon({ name, className, title }) {
  return (
    <svg
      className={className}
      width="1em"
      height="1em"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden={title ? undefined : true}
      role={title ? 'img' : undefined}
      focusable="false"
    >
      {title && <title>{title}</title>}
      {name === 'search' && <line x1="21" y1="21" x2="16.65" y2="16.65" />}
      {PATHS[name]}
    </svg>
  );
}
