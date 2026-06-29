// pacman.js
#!/usr/bin/env node
'use strict';

const blessed = require('blessed');
const fs = require('fs');
const path = require('path');
const os = require('os');

const RECORD_FILE = path.join(os.homedir(), '.pacman_record.json');
function loadRecord() {
    try { return JSON.parse(fs.readFileSync(RECORD_FILE)).record || 0; } catch { return 0; }
}
function saveRecord(record) {
    fs.writeFileSync(RECORD_FILE, JSON.stringify({ record }));
}

let speed = 150;
if (process.argv.includes('-s') && process.argv.length > process.argv.indexOf('-s')+1) {
    speed = parseInt(process.argv[process.argv.indexOf('-s')+1]) || 150;
}

const screen = blessed.screen({
    smartCSR: true,
    title: 'Pac-Man',
    fullUnicode: true,
});
const height = screen.height;
const width = screen.width;
if (height < 22 || width < 42) {
    console.log('Terminal too small (min 22x42)');
    process.exit(1);
}

const H=20, W=20;
let mapData = [
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0],
    [0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0],
    [0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
];
let pacX=9, pacY=9;
let dirX=0, dirY=0;
let score=0, lives=3, level=1;
let best=loadRecord();
let gameOver=false;
let frameTime=speed;

let ghosts = [
    {x:9, y:8, home:[9,8], scared:false},
    {x:8, y:9, home:[8,9], scared:false},
    {x:10, y:9, home:[10,9], scared:false},
    {x:9, y:10, home:[9,10], scared:false},
];
let scaredTimer=0;
let totalDots = mapData.flat().filter(v => v===1 || v===2).length;

function draw() {
    screen.clear();
    for (let y=0; y<H; y++) {
        for (let x=0; x<W; x++) {
            let v = mapData[y][x];
            if (v===0) screen.fillRegion('#', x*2+1, y+1, x*2+2, y+2, blessed.colors.white, blessed.colors.black);
            else if (v===1) screen.fillRegion('.', x*2+1, y+1, x*2+2, y+2, blessed.colors.green, blessed.colors.black);
            else if (v===2) screen.fillRegion('O', x*2+1, y+1, x*2+2, y+2, blessed.colors.cyan, blessed.colors.black);
        }
    }
    screen.fillRegion('C', pacX*2+1, pacY+1, pacX*2+2, pacY+2, blessed.colors.yellow, blessed.colors.black);
    for (const g of ghosts) {
        const color = g.scared ? blessed.colors.blue : blessed.colors.red;
        screen.fillRegion('G', g.x*2+1, g.y+1, g.x*2+2, g.y+2, color, blessed.colors.black);
    }
    screen.setContent(0, 2, `Score: ${score}`, blessed.colors.white);
    screen.setContent(0, W*2-12, `Best: ${best}`, blessed.colors.white);
    screen.setContent(0, W*2-25, `Lives: ${lives}  Level: ${level}`, blessed.colors.white);
    if (gameOver) {
        const msg = 'GAME OVER! Press R to restart, Q to quit';
        screen.setContent(Math.floor(H/2)+1, Math.floor((W*2 - msg.length)/2), msg, blessed.colors.red);
    }
    screen.render();
}

function resetMap() {
    mapData = [
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
        [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
        [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
        [0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0],
        [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
        [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
        [0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0],
        [0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0],
        [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
        [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
        [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
        [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
        [0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0],
        [0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0],
        [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
        [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
        [0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0],
        [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
        [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
        [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    ];
    totalDots = mapData.flat().filter(v => v===1 || v===2).length;
}

function update() {
    if (gameOver) { draw(); return; }

    // Move Pac-Man
    let ny = pacY + dirX;
    let nx = pacX + dirY;
    if (ny>=0 && ny<H && nx>=0 && nx<W && mapData[ny][nx] !== 0) {
        pacY = ny; pacX = nx;
        if (mapData[ny][nx] === 1) { score += 10; mapData[ny][nx] = 3; totalDots--; }
        else if (mapData[ny][nx] === 2) { score += 50; mapData[ny][nx] = 3; totalDots--; scaredTimer = 30; for (const g of ghosts) g.scared = true; }
    }

    // Move ghosts
    const dirs = [[1,0],[-1,0],[0,1],[0,-1]];
    for (const g of ghosts) {
        const idx = Math.floor(Math.random() * 4);
        const [dx,dy] = dirs[idx];
        let nx2 = g.x + dx, ny2 = g.y + dy;
        if (nx2>=0 && nx2<W && ny2>=0 && ny2<H && mapData[ny2][nx2] !== 0) {
            g.x = nx2; g.y = ny2;
        }
    }

    // Collisions
    for (const g of ghosts) {
        if (g.x === pacX && g.y === pacY) {
            if (g.scared) {
                score += 100;
                g.x = g.home[0]; g.y = g.home[1];
                g.scared = false;
            } else {
                lives--;
                if (lives <= 0) {
                    gameOver = true;
                    if (score > best) { best = score; saveRecord(best); }
                } else {
                    pacX = 9; pacY = 9;
                    for (const g2 of ghosts) { g2.x = g2.home[0]; g2.y = g2.home[1]; g2.scared = false; }
                }
            }
            break;
        }
    }

    if (scaredTimer > 0) {
        scaredTimer--;
        if (scaredTimer === 0) for (const g of ghosts) g.scared = false;
    }

    if (totalDots === 0) {
        level++;
        resetMap();
        pacX = 9; pacY = 9;
        for (const g of ghosts) { g.x = g.home[0]; g.y = g.home[1]; g.scared = false; }
    }
    draw();
    setTimeout(update, frameTime);
}

screen.key(['left','a'], function() { dirX=0; dirY=-1; });
screen.key(['right','d'], function() { dirX=0; dirY=1; });
screen.key(['up','w'], function() { dirX=-1; dirY=0; });
screen.key(['down','s'], function() { dirX=1; dirY=0; });
screen.key(['r','R'], function() {
    if (gameOver) {
        resetMap();
        pacX=9; pacY=9; dirX=0; dirY=0;
        score=0; lives=3; level=1; gameOver=false; scaredTimer=0;
        for (const g of ghosts) { g.x=g.home[0]; g.y=g.home[1]; g.scared=false; }
        totalDots = mapData.flat().filter(v => v===1 || v===2).length;
        draw();
    }
});
screen.key(['q','Q'], function() { process.exit(0); });

draw();
update();
