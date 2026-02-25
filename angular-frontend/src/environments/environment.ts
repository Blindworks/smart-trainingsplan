export const environment = {
  production: false,
  apiUrl: `${window.location.protocol}//${window.location.hostname}:${window.location.hostname === 'localhost' ? '8080' : '8081'}/api`
};
