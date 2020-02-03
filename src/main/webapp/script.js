let cells = [];
let state = "";
let isClientA = true;
// in the 1f300-1f3f0 range
let myEmoji = 0;
let opponentEmoji = 1;

function sendCommand(command) {
  let json = fetch('/tictac?'+ command)
	.then(response => response.json());
  json.then(responseJson => processResponse(responseJson));
  return json;
}

function init() {
	let field = document.getElementById("field");
	for (let i = 0; i < 9; i ++) {
		let div = document.createElement("div");
		field.appendChild(div);
		div.onclick = () => clickCell(i);
		cells[i] = div;
	}
	randomizeEmoji();
	sendCommand("initial=true").then(() => {
		// TODO use firebase
		setTimeout(refresh, 1000);
	});
}

function refresh() {
	sendCommand("");
	setTimeout(refresh, 1000);
}

init();

function processResponse(response) {
	document.getElementById("state").innerText = response.state;
	state = response.state;
	isClientA = response.isClientA;
	for (let i = 0; i < 9; i ++) {
		let char = response.fields.charAt(i);
		let result = ' ';
		if (char != ' ') {
			let emoji = ((char == 'o') == isClientA) ? myEmoji : opponentEmoji;
			result = fixedFromCharCode(0x1f300 + emoji);
		}
		cells[i].innerText = result;
	}	
}

function place(index) {
	sendCommand('place=' + index);
}

function clickCell(i) {
	if (cells[i].innerText == ' ' && state == 'MyTurn') {
		place(i);
	}
}

function abandon() {
	sendCommand('abandon=true');
	randomizeEmoji();
}

function randomizeEmoji() {
	myEmoji = Math.floor(Math.random() * 240);
	do {
		opponentEmoji = Math.floor(Math.random() * 240);
	} while (myEmoji == opponentEmoji);
	document.getElementById("my-piece").innerText = fixedFromCharCode(0x1f300 + myEmoji);
}

function fixedFromCharCode (codePt) {
    if (codePt > 0xFFFF) {
        codePt -= 0x10000;
        return String.fromCharCode(0xD800 + (codePt >> 10), 0xDC00 + (codePt & 0x3FF));
    } else {
        return String.fromCharCode(codePt);
    }
}