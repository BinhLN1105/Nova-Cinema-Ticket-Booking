import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const srcDir = path.join(process.cwd(), 'src');

function getAllFiles(dirPath, arrayOfFiles) {
  const files = fs.readdirSync(dirPath);

  arrayOfFiles = arrayOfFiles || [];

  files.forEach(function(file) {
    if (fs.statSync(path.join(dirPath, file)).isDirectory()) {
      arrayOfFiles = getAllFiles(path.join(dirPath, file), arrayOfFiles);
    } else {
      arrayOfFiles.push(path.join(dirPath, file));
    }
  });

  return arrayOfFiles;
}

const files = getAllFiles(srcDir);
const tsFiles = files.filter(f => f.endsWith('.ts') || f.endsWith('.tsx'));

console.log(`Found ${tsFiles.length} files to convert.`);

for (const file of tsFiles) {
  console.log(`Converting ${file}...`);
  try {
    execSync(`npx -y detype "${file}"`, { stdio: 'pipe' });
    fs.unlinkSync(file);
    console.log(`Deleted original ${file}`);
  } catch (err) {
    console.error(`Failed to convert ${file}`, err.message);
  }
}
