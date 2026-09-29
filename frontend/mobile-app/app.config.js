// The Android Maps SDK key is embedded in native builds, not in JavaScript.
// Expo Go supplies its own test configuration; standalone builds need this key.
module.exports = ({ config }) => {
  const mapsKey = process.env.GOOGLE_MAPS_ANDROID_API_KEY;
  if (process.env.EAS_BUILD_PROFILE === 'production') {
    if (!mapsKey) throw new Error('Production Android build requires GOOGLE_MAPS_ANDROID_API_KEY. Configure it in the EAS production environment.');
    if (!/^https:\/\//.test(process.env.EXPO_PUBLIC_API_BASE_URL || '')) throw new Error('Production Android build requires an HTTPS EXPO_PUBLIC_API_BASE_URL.');
    if (!/^https:\/\//.test(process.env.EXPO_PUBLIC_CUSTOMER_PORTAL_URL || '')) throw new Error('Production Android build requires an HTTPS EXPO_PUBLIC_CUSTOMER_PORTAL_URL for privacy and deletion pages.');
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(process.env.EXPO_PUBLIC_DEFAULT_TENANT_ID || '')) throw new Error('Production Android build requires an approved EXPO_PUBLIC_DEFAULT_TENANT_ID workspace UUID.');
  }
  return {
    ...config,
    plugins: [
      ...(config.plugins || []),
      ...(mapsKey ? [['react-native-maps', { androidGoogleMapsApiKey: mapsKey }]] : []),
    ],
  };
};
