#!/usr/bin/env node
// Computes per-function McCabe cyclomatic complexity across a TypeScript source
// tree, using the same decision-point rules as ESLint's `complexity` rule:
// +1 per function/method, then +1 for each if, ternary, loop, catch, switch
// case, and short-circuit (&&, ||, ??). Used by the code-reviewer skill's
// complexity check (SKILL.md §18) — see that section for the max-15 threshold.
// The backend (Java) equivalent is `mvn pmd:check` in backend/, configured
// via backend/pmd-complexity-ruleset.xml with the same threshold.
//
// Usage: node scripts/cyclomatic-complexity.js [src/app] [--max=15]

const ts = require('typescript');
const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const maxArg = args.find((a) => a.startsWith('--max='));
const maxComplexity = maxArg ? Number(maxArg.split('=')[1]) : 15;
const root = path.resolve(args.find((a) => !a.startsWith('--')) || 'src/app');

function walk(dir, acc) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, acc);
    } else if (
      entry.isFile() &&
      entry.name.endsWith('.ts') &&
      !entry.name.endsWith('.cy.ts') &&
      !entry.name.endsWith('.spec.ts')
    ) {
      acc.push(full);
    }
  }
  return acc;
}

const files = walk(root, []);

const FUNCTION_KINDS = new Set([
  ts.SyntaxKind.FunctionDeclaration,
  ts.SyntaxKind.FunctionExpression,
  ts.SyntaxKind.ArrowFunction,
  ts.SyntaxKind.MethodDeclaration,
  ts.SyntaxKind.Constructor,
  ts.SyntaxKind.GetAccessor,
  ts.SyntaxKind.SetAccessor,
]);

function functionName(node, sourceFile) {
  if (node.name && node.name.getText) return node.name.getText(sourceFile);
  if (ts.isConstructorDeclaration(node)) return 'constructor';
  const parent = node.parent;
  if (parent) {
    if (ts.isVariableDeclaration(parent) && parent.name) return parent.name.getText(sourceFile);
    if (ts.isPropertyAssignment(parent) && parent.name) return parent.name.getText(sourceFile);
    if (ts.isPropertyDeclaration(parent) && parent.name) return parent.name.getText(sourceFile);
    if (ts.isCallExpression(parent)) return '<arg-callback>';
  }
  return '<anonymous>';
}

function computeComplexity(node) {
  let complexity = 1;

  function visit(n) {
    switch (n.kind) {
      case ts.SyntaxKind.IfStatement:
      case ts.SyntaxKind.ConditionalExpression:
      case ts.SyntaxKind.ForStatement:
      case ts.SyntaxKind.ForInStatement:
      case ts.SyntaxKind.ForOfStatement:
      case ts.SyntaxKind.WhileStatement:
      case ts.SyntaxKind.DoStatement:
      case ts.SyntaxKind.CatchClause:
      case ts.SyntaxKind.CaseClause:
        complexity++;
        break;
      case ts.SyntaxKind.BinaryExpression: {
        const op = n.operatorToken.kind;
        if (
          op === ts.SyntaxKind.AmpersandAmpersandToken ||
          op === ts.SyntaxKind.BarBarToken ||
          op === ts.SyntaxKind.QuestionQuestionToken
        ) {
          complexity++;
        }
        break;
      }
      default:
        break;
    }
    // Nested function-like nodes are counted separately; don't descend into them.
    if (n !== node && FUNCTION_KINDS.has(n.kind)) {
      return;
    }
    ts.forEachChild(n, visit);
  }

  ts.forEachChild(node, visit);
  return complexity;
}

const results = [];

for (const file of files) {
  const text = fs.readFileSync(file, 'utf8');
  const sourceFile = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS);
  const rel = path.relative(process.cwd(), file).replace(/\\/g, '/');

  function visitTop(node) {
    if (FUNCTION_KINDS.has(node.kind) && node.body) {
      const complexity = computeComplexity(node);
      const { line } = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
      results.push({ file: rel, name: functionName(node, sourceFile), line: line + 1, complexity });
    }
    ts.forEachChild(node, visitTop);
  }

  visitTop(sourceFile);
}

results.sort((a, b) => b.complexity - a.complexity);

const offenders = results.filter((r) => r.complexity > maxComplexity);

console.log(`Scanned ${files.length} files, ${results.length} functions under ${path.relative(process.cwd(), root)}.`);
console.log(`Complexity threshold: max ${maxComplexity} per function.\n`);

if (offenders.length === 0) {
  console.log(`No functions exceed complexity ${maxComplexity}.`);
} else {
  console.log(`${offenders.length} function(s) exceed complexity ${maxComplexity}:\n`);
  for (const o of offenders) {
    console.log(`  ${o.complexity}\t${o.file}:${o.line}\t${o.name}`);
  }
}

process.exitCode = offenders.length > 0 ? 1 : 0;
