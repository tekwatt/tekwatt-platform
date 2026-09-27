// The Android Maps SDK key is embedded in native builds, not in JavaScript.
// Expo Go supplies its own test configuration; standalone builds need this key.
module.exports = ({ config }) => {
  const mapsKey = process.env.GOOGLE_MAPS_ANDROID_API_KEY;
  return {
    ...config,
    plugins: [
      ...(config.plugins || []),
      ...(mapsKey ? [['react-native-maps', { androidGoogleMapsApiKey: mapsKey }]] : []),
    ],
  };
};
