interface SquareConfig {
    color: string;
    width: number;
}

function createSquare(config: SquareConfig): void {
    // ...
}

let mySquare = createSquare({ colour: "red", width: 100 });