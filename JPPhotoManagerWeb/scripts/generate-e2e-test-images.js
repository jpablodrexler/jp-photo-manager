#!/usr/bin/env node
// Generates small, valid, solid-color PNG files with no external dependencies
// (no ImageMagick/ffmpeg/Pillow required) for seeding the release-e2e-suite's
// dedicated /e2e-catalog test folder with real, distinguishable image assets.
//
// Usage: node generate-e2e-test-images.js <output-dir>
//
// Writes a fixed manifest of images (see IMAGES below): distinct solid
// colors (so a mis-cataloged/mixed-up thumbnail is visually obvious, the
// same technique CatalogBatchConcurrencyIntegrationTest.java uses on the
// backend side), plus one exact byte-for-byte duplicate to exercise the
// Duplicates feature, across a couple of subfolders to exercise folder
// navigation.

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const WIDTH = 64;
const HEIGHT = 64;

// IEEE 802.3 CRC-32, precomputed table — Node's zlib module compresses but
// does not expose crc32, and PNG chunks require one.
const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  }
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const typeBuf = Buffer.from(type, 'ascii');
  const lenBuf = Buffer.alloc(4);
  lenBuf.writeUInt32BE(data.length, 0);
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([lenBuf, typeBuf, data, crcBuf]);
}

// Encodes a solid-color, width x height, 8-bit truecolor (RGB, color type 2)
// PNG — the minimal pixel format every PNG decoder supports, including
// Java's ImageIO (used by StoragePort.generateThumbnail/getExifMetadata).
function encodeSolidColorPng(width, height, [r, g, b]) {
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(width, 0);
  ihdrData.writeUInt32BE(height, 4);
  ihdrData[8] = 8; // bit depth
  ihdrData[9] = 2; // color type: truecolor (RGB)
  ihdrData[10] = 0; // compression method
  ihdrData[11] = 0; // filter method
  ihdrData[12] = 0; // interlace method
  const ihdr = chunk('IHDR', ihdrData);

  // Raw image data: each scanline is a filter-type byte (0 = none) followed
  // by width * 3 raw RGB bytes.
  const raw = Buffer.alloc(height * (1 + width * 3));
  let offset = 0;
  for (let y = 0; y < height; y++) {
    raw[offset++] = 0; // filter type: none
    for (let x = 0; x < width; x++) {
      raw[offset++] = r;
      raw[offset++] = g;
      raw[offset++] = b;
    }
  }
  const idat = chunk('IDAT', zlib.deflateSync(raw));

  const iend = chunk('IEND', Buffer.alloc(0));

  return Buffer.concat([signature, ihdr, idat, iend]);
}

// name (relative to output dir, subfolders allowed) -> [r, g, b]
const IMAGES = {
  'trip/red-square.png': [220, 20, 20],
  'trip/green-square.png': [20, 220, 20],
  'trip/blue-square.png': [20, 20, 220],
  'events/yellow-square.png': [220, 220, 20],
  'events/magenta-square.png': [220, 20, 220],
  // Exact duplicate of trip/red-square.png (same pixels -> same hash) to
  // exercise the Duplicates feature.
  'events/red-square-duplicate.png': [220, 20, 20],
};

function main() {
  const outDir = process.argv[2];
  if (!outDir) {
    console.error('Usage: node generate-e2e-test-images.js <output-dir>');
    process.exit(1);
  }

  const written = [];
  for (const [relPath, color] of Object.entries(IMAGES)) {
    const fullPath = path.join(outDir, relPath);
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    fs.writeFileSync(fullPath, encodeSolidColorPng(WIDTH, HEIGHT, color));
    written.push(fullPath);
  }

  console.log(`Wrote ${written.length} test images under ${outDir}:`);
  written.forEach((p) => console.log(`  ${p}`));
}

main();
