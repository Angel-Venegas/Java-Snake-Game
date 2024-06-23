package org.snakegame.snake;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;


public class Main extends Application {
    private static final int GRID_SIZE = 50; // Includes the same width and height
    private static final int CELL_SIZE = 20; // Includes the same width and height
    private Group gridRoot;
    private final Group snake = new Group();
    private Direction direction = Direction.RIGHT;
    private final AtomicBoolean gameRunning = new AtomicBoolean(true);
    private Timeline timeline;
    private int score = 0;
    private Text scoreText;

    private enum Direction { // To set the current direction to one of these four
        UP, RIGHT, DOWN, LEFT
    }

    private void createGrid() { // Creates the background grid with color
        for (int y = 0; y < GRID_SIZE; y++) { // Columns
            for (int x = 0; x < GRID_SIZE * 2; x++) { // Rows
                // Create a Rectangle at position (x * CELL_SIZE, y * CELL_SIZE) with dimensions CELL_SIZE x CELL_SIZE
                Rectangle cell = new Rectangle(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);


                // Set RGB colors for alternating cells
                if ((x + y) % 2 == 0) {
                    cell.setFill(Color.BLANCHEDALMOND);
                } else {
                    cell.setFill(Color.SILVER); // Set to RGB color for lighter gray (e.g., black)
                }

                gridRoot.getChildren().add(cell);
            }
        }
    }

    private void initializeScoreText() {
        scoreText = new Text("Score: " + score);
        scoreText.setFill(Color.BLACK);
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        scoreText.setX(GRID_SIZE * CELL_SIZE - 80);
        scoreText.setY(20);
        gridRoot.getChildren().add(scoreText);
    }


    private void createSnake() { // Creates the starting snake on the same positions of our grid canvas
        int SNAKE_INITIAL_SIZE = 0;

        for (int i = SNAKE_INITIAL_SIZE; i >= 0; --i) { // Builds the snake backwards and the head of the snake is the first iteration
            // Rectangle(double x, double y, double width, double height)
            // x and y coordinates represent the individual pixels on your canvas
            Rectangle segment = new Rectangle(i * CELL_SIZE, 0, CELL_SIZE, CELL_SIZE);
            segment.setFill(Color.DEEPSKYBLUE);
            snake.getChildren().add(segment);
        }
    }

    private void generateFood() {
        Random random = new Random();

        int posX;
        int posY;

        do {
            posX = random.nextInt(GRID_SIZE);
            posY = random.nextInt(GRID_SIZE);
        } while (checkCollision(posX, posY)); // Keep looping until there is no collision with the snake, when not, generate the apple

        // Now posX and posY are valid positions for the food
        // Create an ImageView for the apple image
        Image appleImage = new Image("org/snakegame/snake/Images/Apple.png");
        ImageView appleImageView = new ImageView(appleImage);
        appleImageView.setFitWidth(CELL_SIZE);
        appleImageView.setFitHeight(CELL_SIZE);
        appleImageView.setLayoutX(posX * CELL_SIZE);
        appleImageView.setLayoutY(posY * CELL_SIZE);

        gridRoot.getChildren().add(appleImageView);

        // Increase the score when generating a new apple
        score++;
        updateScoreText();
    }
    private void updateScoreText() {
        scoreText.setText("Score: " + score);
    }
    //   |
    //   |
    //  \ /
    //   .
    private void handleKeyPress(KeyCode code) { // Handles the arrow key movements and prevents opposite direction movements
        // Short For
        /*
        Direction newDirection = null;

        switch (code) {
            case LEFT:
                newDirection = Direction.LEFT;
                break;
            case UP:
                newDirection = Direction.UP;
                break;
            case RIGHT:
                newDirection = Direction.RIGHT;
                break;
            case DOWN:
                newDirection = Direction.DOWN;
                break;
        }
        */
        Direction newDirection = switch (code) {
            case LEFT -> Direction.LEFT;
            case UP -> Direction.UP;
            case RIGHT -> Direction.RIGHT;
            case DOWN -> Direction.DOWN;
            default -> null;
            // Determine the new direction based on the pressed key
        };

        // Check if the new direction is not opposite to the current direction
        if (newDirection != null && !isOppositeDirection(newDirection, direction)) {
            direction = newDirection;
        }
    }
    //   |
    //   |
    //  \ /
    //   .
    private boolean isOppositeDirection(Direction newDirection, Direction currDirection) { // Returns true if the new direction is opposite
        return (newDirection == Direction.LEFT && currDirection == Direction.RIGHT) ||
                (newDirection == Direction.RIGHT && currDirection == Direction.LEFT) ||
                (newDirection == Direction.UP && currDirection == Direction.DOWN) ||
                (newDirection == Direction.DOWN && currDirection == Direction.UP);
    }

    private void moveSnake() { // To update the movement of the snake based on what the current direction is set ot
        if (!gameRunning.get()) { // If false is returned by gameRunning.get() then the expression becomes true which means the game is not running
            return;
        }

        double deltaX = 0;
        double deltaY = 0;

        switch (direction) {
            case UP:
                deltaY -= CELL_SIZE;
                break;
            case DOWN:
                deltaY += CELL_SIZE;
                break;
            case LEFT:
                deltaX -= CELL_SIZE;
                break;
            case RIGHT:
                deltaX += CELL_SIZE;
                break;
        }

        // Moves the head to the desired pixel position
        Rectangle head = shiftSnakeBoxes(); // Get the current head of the snake
        double newHeadX = head.getX() + deltaX; // and then set it to the nex x and y position on the canvas
        double newHeadY = head.getY() + deltaY;

        // Checks for collisions of the new head position with the snake's body before it is set
        if (checkCollision(newHeadX, newHeadY)) {
            gameOver();
            return;
        }

        head.setX(newHeadX);
        head.setY(newHeadY);

        // Check if the new head position collides with the apple
        if (checkAppleCollision(newHeadX, newHeadY)) {
            // Increase snake length
            Rectangle newSegment = new Rectangle(-1, -1, CELL_SIZE, CELL_SIZE);
            newSegment.setFill(Color.DEEPSKYBLUE);
            snake.getChildren().add(newSegment);

            // Generate a new apple
            generateFood();
        }
    }
    //   |
    //   |
    //  \ /
    //   .
    private boolean checkAppleCollision(double x, double y) {
        ObservableList<Node> gridItems = gridRoot.getChildren();

        // Check if the new head position is exactly at the apple's position
        for (Node item : gridItems) {
            if (item instanceof ImageView appleImageView) {
                if (Math.abs(appleImageView.getBoundsInParent().getMinX() - x) < 1.0
                        && Math.abs(appleImageView.getBoundsInParent().getMinY() - y) < 1.0) {
                    // Collision with apple detected
                    gridRoot.getChildren().remove(appleImageView); // Remove the eaten apple
                    return true;
                }
            }
        }

        return false; // No collision with apple
    }
    //   |
    //   |
    //  \ /
    //   .
    private boolean checkCollision(double x, double y) { // Checks if the current head's position is in the same position of a body part of the snake or if it is outside the grid as well
        ObservableList<Node> snakeSegments = snake.getChildren();

        // Check if the new head position is outside the grid boundaries
        if (x < 0 || x >= GRID_SIZE * CELL_SIZE || y < 0 || y >= GRID_SIZE * CELL_SIZE) {
            return true; // Collision with the grid boundaries
        }

        // Check if the new head position collides with any part of the snake's body
        for (Node segment : snakeSegments) {
            Rectangle bodySegment = (Rectangle) segment;
            if (bodySegment.getX() == x && bodySegment.getY() == y) {
                return true; // Collision detected
            }
        }

        return false; // No collision
    }
    //   |
    //   |
    //  \ /
    //   .
    private Rectangle shiftSnakeBoxes() {
        ObservableList<Node> snakeSegments = snake.getChildren();

        // The loop starts at the tail (last segment) and iterates towards the head (first segment).
        for (int i = snakeSegments.size() - 1; i > 0; i--) { // Because nodes are indexed from 0 to n - 1
            Rectangle currentSegment = (Rectangle) snakeSegments.get(i); // Gets the current box
            Rectangle adjacentSegment = (Rectangle) snakeSegments.get(i - 1); // Gets the box after the current one, which is the one closer to the head

            // Moves the current box's position to the next position adjacent to it
            currentSegment.setX(adjacentSegment.getX());
            currentSegment.setY(adjacentSegment.getY());
        } // Basically moves everything to the right one box, and the tail is "consumed" by the adjacent segment. Everything rectangle is set to the next position of x and y except for the head

        // The loop handles all segments except the head, and the head is retrieved separately at the end of the method.
        return (Rectangle) snakeSegments.getFirst();
    }

    private void gameOver() {
         Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText("Your Score: " + score);

            alert.setOnHidden(event -> {
                // Stop the timeline when the alert is closed and terminate the JavaFX thread
                timeline.stop();
                Platform.exit();
            });

            alert.showAndWait();
        });

        gameRunning.set(false);
    }



    private void startGameLoop() { // Calls moveSnake() every 100 milliseconds
        timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            moveSnake();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); // For an infinite cycle time in key frames

        timeline.setOnFinished(event -> { // The timeline stops when .stop() is called or the JavaFX thread is terminated or the program is terminated and then calls this lambda function
            gameRunning.set(false);
            Platform.exit();
        });

        timeline.play();
    }



    @Override
    public void start(Stage stage) { // Starts the JavaFX thread with stage, scene, and event handlers
        gridRoot = new Group();
        Scene scene = new Scene(gridRoot, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE); // Root Node, Width, Height

        // Load the image
        Image snakeIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/org/snakegame/snake/Images/Snake.png")));
        stage.getIcons().add(snakeIcon);

        createGrid();
        createSnake();
        initializeScoreText();
        generateFood();
        gridRoot.getChildren().add(snake);


        Button startButton = new Button("Start Game");
        startButton.setScaleX(5);
        startButton.setScaleY(5);
        startButton.translateXProperty().bind(scene.widthProperty().subtract(startButton.widthProperty()).divide(2));
        startButton.translateYProperty().bind(scene.heightProperty().subtract(startButton.heightProperty()).divide(2));

        startButton.setOnAction(e -> {
            gridRoot.getChildren().remove(startButton); // Remove the button from the scene
            startGameLoop(); // Start the game loop
        });


        scene.setOnKeyPressed(event -> {
            handleKeyPress(event.getCode());
        });

        stage.setTitle("Snake Game");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // Add the button to the scene
        gridRoot.getChildren().add(startButton);
    }


    public static void main(String[] args) {
        launch();
    }
}


// Bugs
/*
There is a bug where the snake changes to the opposite direction when the user clicks an arrow that is not opposite to the current direction, lets call the "current direction" A, of the snake
and then quickly clicks the arrow opposite direction A before a new Key Frame is generated. The user would have to be extremely quick but the bug is there.
*/


// Notes:
/*
In Java, a boolean variable is not inherently atomic. This means that if multiple threads attempt to read and write to a boolean variable concurrently,
unexpected behavior can occur due to thread interference. For example, one thread might read the variable while another is in the process of updating it,
leading to inconsistent or incorrect results.


alert.setOnHidden(event -> {
    timeline.stop();
    Platform.exit();
});
In JavaFX, the setOnHidden method is used to set an event handler that will be invoked when the window or dialog (in this case, an Alert) is closed or hidden.
The setOnHidden method takes an event handler as a parameter, and this event handler will be called when the Alert is closed, either by the user or programmatically.

In JavaFX, Platform.exit() is a method provided by the javafx.application.Platform class. This method is used to terminate the JavaFX application. When called, it initiates
the JavaFX application shutdown process, which involves stopping the JavaFX application thread and releasing any resources associated with the JavaFX application.


Platform.runLater():
In JavaFX, the Platform.runLater() method is used to execute a piece of code on the JavaFX Application Thread. JavaFX follows a single-threaded model where all UI-related operations should be performed on the JavaFX Application Thread.
This is to ensure thread safety and avoid potential issues that may arise from concurrent access to UI components.

The Platform.runLater() method is typically used when you need to update the UI or perform some UI-related task from a background thread. If you attempt to update the UI directly from a non-JavaFX thread,
you may encounter ConcurrentModificationException or other threading-related issues.
*/