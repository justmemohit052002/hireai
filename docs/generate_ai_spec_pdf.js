const { spawnSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const htmlPath = path.resolve(__dirname, 'HireAI_AI_Engine_Architecture_and_Integration_Specification.html');
const pdfPath = path.resolve(__dirname, 'HireAI_AI_Engine_Architecture_and_Integration_Specification.pdf');
const rootPdfPath = path.resolve(__dirname, '..', 'HireAI_AI_Engine_Architecture_and_Integration_Specification.pdf');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const browserExe = fs.existsSync(chromePath) ? chromePath : (fs.existsSync(edgePath) ? edgePath : null);

if (!browserExe) {
  console.error('ERROR: Neither Chrome nor Edge executable was found.');
  process.exit(1);
}

const fileUrl = 'file:///' + htmlPath.replace(/\\/g, '/');

console.log('Source HTML:', htmlPath);
console.log('Target PDF:', pdfPath);
console.log('Browser Exe:', browserExe);

const args = [
  '--headless=new',
  '--disable-gpu',
  '--no-pdf-header-footer',
  `--print-to-pdf=${pdfPath}`,
  fileUrl
];

const result = spawnSync(browserExe, args, { stdio: 'inherit' });

if (fs.existsSync(pdfPath)) {
  const size = fs.statSync(pdfPath).size;
  console.log(`\nSUCCESS: Generated PDF specification document successfully!`);
  console.log(`File: ${pdfPath}`);
  console.log(`Size: ${(size / 1024).toFixed(1)} KB`);
  
  // Also copy to root directory for easy access
  fs.copyFileSync(pdfPath, rootPdfPath);
  console.log(`Copied to: ${rootPdfPath}`);
} else {
  console.error('ERROR: Failed to generate PDF file. Result status:', result.status);
}
