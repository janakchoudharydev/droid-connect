const express = require('express');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const ICONS_DIR = path.join(__dirname, 'icons');
const TERMINAL_ICNS_PATH = '/opt/homebrew/Cellar/terminal-notifier/3.1.0/terminal-notifier.app/Contents/Resources/Terminal.icns';

app.use(express.json());

// Package name to .icns filename mapping
const PACKAGE_ICNS_MAP = {
  'com.whatsapp': 'whatsapp.icns',
  'com.whatsapp.w4b': 'whatsapp.icns',
  'org.telegram.messenger': 'telegram.icns',
  'org.thunderdog.chatterino': 'telegram.icns',
  'com.google.android.gm': 'gmail.icns',
  'com.google.android.googlequicksearchbox': 'google.icns',
  'com.motorola.timeweatherwidget': 'google.icns',
  'com.google.android.apps.messaging': 'messages.icns',
  'com.android.mms': 'messages.icns',
  'com.google.android.dialer': 'messages.icns',
  'com.google.android.apps.photos': 'instagram.icns',
  'com.google.android.apps.maps': 'maps.icns',
  'com.google.android.youtube': 'youtube.icns',
  'com.instagram.android': 'instagram.icns',
  'com.twitter.android': 'twitter.icns',
  'com.spotify.music': 'spotify.icns'
};

const NOTIFIERS_DIR = path.join(__dirname, 'notifiers');

function resolveNotifierBinary(packageName, appName) {
  const lowerPkg = (packageName || '').toLowerCase();
  const lowerName = (appName || '').toLowerCase();

  let appNameFolder = 'AntiGravity.app';

  if (lowerPkg.includes('whatsapp') || lowerName.includes('whatsapp')) appNameFolder = 'WhatsApp.app';
  else if (lowerPkg.includes('telegram') || lowerName.includes('telegram')) appNameFolder = 'Telegram.app';
  else if (lowerPkg.includes('gmail') || lowerPkg.includes('mail') || lowerName.includes('mail')) appNameFolder = 'Gmail.app';
  else if (lowerPkg.includes('google') || lowerPkg.includes('weather') || lowerName.includes('google')) appNameFolder = 'Google.app';
  else if (lowerPkg.includes('message') || lowerPkg.includes('sms') || lowerName.includes('message')) appNameFolder = 'Messages.app';
  else if (lowerPkg.includes('instagram') || lowerName.includes('instagram')) appNameFolder = 'Instagram.app';
  else if (lowerPkg.includes('youtube') || lowerName.includes('youtube')) appNameFolder = 'YouTube.app';
  else if (lowerPkg.includes('spotify') || lowerName.includes('spotify')) appNameFolder = 'Spotify.app';
  else if (lowerPkg.includes('map') || lowerName.includes('map')) appNameFolder = 'Maps.app';
  else if (lowerPkg.includes('twitter') || lowerName.includes('twitter')) appNameFolder = 'Twitter.app';

  const binPath = path.join(NOTIFIERS_DIR, appNameFolder, 'Contents', 'MacOS', 'terminal-notifier');
  return {
    notifierBinary: fs.existsSync(binPath) ? binPath : 'terminal-notifier',
    appNameFolder
  };
}

function resolveAppIcon(packageName, appName) {
  const lowerPkg = (packageName || '').toLowerCase();
  const lowerName = (appName || '').toLowerCase();

  if (lowerPkg.includes('whatsapp') || lowerName.includes('whatsapp')) return path.join(ICONS_DIR, 'whatsapp.png');
  if (lowerPkg.includes('telegram') || lowerName.includes('telegram')) return path.join(ICONS_DIR, 'telegram.png');
  if (lowerPkg.includes('gmail') || lowerPkg.includes('mail') || lowerName.includes('mail')) return path.join(ICONS_DIR, 'gmail.png');
  if (lowerPkg.includes('google') || lowerPkg.includes('weather') || lowerName.includes('google')) return path.join(ICONS_DIR, 'google.png');
  if (lowerPkg.includes('message') || lowerPkg.includes('sms') || lowerName.includes('message')) return path.join(ICONS_DIR, 'messages.png');
  if (lowerPkg.includes('instagram') || lowerName.includes('instagram')) return path.join(ICONS_DIR, 'instagram.png');
  if (lowerPkg.includes('youtube') || lowerName.includes('youtube')) return path.join(ICONS_DIR, 'youtube.png');
  if (lowerPkg.includes('spotify') || lowerName.includes('spotify')) return path.join(ICONS_DIR, 'spotify.png');
  if (lowerPkg.includes('map') || lowerName.includes('map')) return path.join(ICONS_DIR, 'maps.png');
  if (lowerPkg.includes('twitter') || lowerName.includes('twitter')) return path.join(ICONS_DIR, 'twitter.png');

  const defaultPng = path.join(ICONS_DIR, 'default.png');
  return fs.existsSync(defaultPng) ? defaultPng : null;
}

// POST /notify - Receives Android notifications and displays them on macOS
app.post('/notify', (req, res) => {
  const { packageName, appName, title, text } = req.body;

  const displayTitle = (title || appName || 'Android Notification').trim();
  const displaySubtitle = (appName || packageName || '').trim();
  let displayMessage = (text || '').trim();

  // terminal-notifier requires a non-empty -message value
  if (!displayMessage) {
    displayMessage = displayTitle || 'New notification';
  }

  // terminal-notifier parser bug: if first char is [ { ( ", prepend a space
  if (/^[\[\(\{"']/.test(displayMessage)) {
    displayMessage = ' ' + displayMessage;
  }

  const { notifierBinary, appNameFolder } = resolveNotifierBinary(packageName, appName);

  // Build native Apple notification command
  const args = [
    '-title', displayTitle,
    '-message', displayMessage,
    '-sound', 'default'
  ];

  if (displaySubtitle) {
    args.push('-subtitle', displaySubtitle);
  }

  // Group by package + sender/chat
  if (packageName) {
    const safeTitleSlug = displayTitle.replace(/[^a-zA-Z0-9]/g, '_').slice(0, 20);
    args.push('-group', `${packageName}_${safeTitleSlug}`);
  }

  console.log(`[Notification via ${appNameFolder}] "${displayTitle}": ${displayMessage}`);

  const notifierProcess = spawn(notifierBinary, args);

  notifierProcess.on('error', (err) => {
    console.error(`notifier error: ${err.message}`);
  });

  notifierProcess.on('exit', (code) => {
    if (code !== 0) {
      console.warn(`notifier exited with code ${code}`);
    }
  });

  return res.status(200).json({ success: true, timestamp: Date.now() });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Anti Gravity macOS receiver listening on http://0.0.0.0:${PORT}`);
  console.log(`Ready to receive Android notifications over local Wi-Fi.`);
});
