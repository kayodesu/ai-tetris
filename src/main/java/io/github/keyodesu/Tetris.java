package io.github.keyodesu;

import io.github.keyodesu.ai.AI;
import io.github.keyodesu.ai.ElTetris;
import io.github.keyodesu.block.Block;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ResourceBundle;
import java.util.concurrent.LinkedBlockingQueue;

import static io.github.keyodesu.Tetris.Action.*;

/**
 * @author Yo Ka
 */
public class Tetris extends Application {

    private volatile boolean isGameOver = false; // 一局游戏结束
    private volatile boolean isInPause = false; // 游戏是否在暂停中
    private boolean isMute = false;  // 是否静音
//    private MediaPlayer player; // 用来播放背景音乐

    private Container gameContainer;
    private Container nextBlockContainer;
    private boolean aiPlaying = false;
    private AI ai;

    private int currScore = 0;
    private int currLevel = 1;

    // 控制小方块运动的所有动作
    public enum Action {
        LEFT, RIGHT, DOWN, TRANSFORM, FAST_DOWN
    }

    // LinkedBlockingQueue是线程安全的
    private LinkedBlockingQueue<Action> actionQueue = new LinkedBlockingQueue<>(1024);

    /*
     * 自动添加向下运动事件的线程
     * 每间隔一定的时间，自动向消息队列中添加一个向下运动的事件，来实现小方块自动向下运动
     */
    Thread autoDownThread = new Thread(() -> {
        /*
         * 小方块下落的时间间隔
         * 数组下标表示当前等级，数组值表示此等级下下落的时间间隔（毫秒）
         * 最高10级（1-10）
         */
        int[] downRate = new int[11];
        for(int i = 1; i < 11; i++) {
            downRate[i] = 1000 - (i-1) * 100;
        }

        while (!isGameOver) {
            try {
                if (!isInPause && !aiPlaying) {
                    actionQueue.put(Action.DOWN);
                }

                if (currLevel >= 1 && currLevel <= 10) {
                    Thread.sleep(downRate[currLevel]);
                } else {
                    throw new NeverReachHereError("currLevel = " + currLevel);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    private static final int FAST_DOWN_CELL_COUNT = 3;

    /*
     * 工作线程
     * 从消息队列中取消息，然后处理消息。
     */
    Thread workThread = new Thread(new Runnable() {
        private static final int BEGIN_X = 3;  // 默认小方块从第3列出来
        private static final int BEGIN_Y = -Block.SIDE_LEN;

        private Block nextBlock;

        /**
         * 触底后的处理
         */
        private void touchBottom() {
            int eliminatedLinesCount = gameContainer.merger();
            if(eliminatedLinesCount > 0) {
                System.out.println("eliminated " + eliminatedLinesCount + " row(s)");

                if (eliminatedLinesCount == 1)
                    currScore += 10;   //  一次消除一层： 获得10积分
                else if (eliminatedLinesCount == 2)
                    currScore += 30;   //  一次消除二层： 获得30积分
                else if (eliminatedLinesCount == 3)
                    currScore += 60;   //  一次消除三层： 获得60积分
                else if (eliminatedLinesCount == 4)
                    currScore += 100;  //  一次消除四层： 获得100积分
                else
                    throw new NeverReachHereError("error. Eliminated " + eliminatedLinesCount + " row(s)");

//                if(currScore > heightestScore) {
//                    heightestScore = currScore;
//                    Editor editor = sp.edit();
//                    editor.putInt("HeightestScore", heightestScore);
//                    editor.commit();
//                }

                // 根据当前得分确定等级
                if(currScore < 1000)
                    currLevel = 1;
                else if(currScore < 3000)
                    currLevel = 2;
                else if(currScore < 6000)
                    currLevel = 3;
                else if(currScore < 10000)
                    currLevel = 4;
                else if(currScore < 15000)
                    currLevel = 5;
                else if(currScore < 21000)
                    currLevel = 6;
                else if(currScore < 28000)
                    currLevel = 7;
                else if(currScore < 36000)
                    currLevel = 8;
                else if(currScore < 45000)
                    currLevel = 9;
                else currLevel = 10;

                // 将更新界面的工作交给 FX application thread 执行
                Platform.runLater(() -> {
                    scoreLabel.setText(String.valueOf(currScore));
                    levelLabel.setText(String.valueOf(currLevel));
                    speedLabel.setText("unknown"); // todo
                });
            }

            gameContainer.setDanglingBlock(BEGIN_X, BEGIN_Y, nextBlock);
            nextBlock = Block.getRandomBlock();

            nextBlockContainer.setDanglingBlock(0, 0, nextBlock);
            nextBlockContainer.draw();
//            nextBlockPanel.setBlockStat(0, 0, 4, 4, nextBlock.getData());
//            nextBlockPanel.drawPanel();

            actionQueue.clear();
            if (gameContainer.isFull())
                gameOver();
        }

        @Override
        public void run() {
            gameContainer.setDanglingBlock(BEGIN_X, BEGIN_Y, Block.getRandomBlock());

            nextBlock = Block.getRandomBlock();
            nextBlockContainer.setDanglingBlock(0, 0, nextBlock);
            nextBlockContainer.draw();

            while (!isGameOver) {
                try {
                    if (aiPlaying) {
                        ai.calBestColAndStat();
                        while (gameContainer.moveDown()) {
                            Thread.sleep(10);
                        }
                        touchBottom();
                    } else {
//                    nextBlockPanel.setBlockStat(0, 0, 4, 4, nextBlock.getData());  todo
//                    nextBlockPanel.drawPanel();

                        Action action = null;

                        while (action == null && !aiPlaying) {
                            if (isGameOver) {
                                return;
                            }

                            // 循环取消息直到取出消息为止 注意，取消息不能用阻塞函数actionQueue.take()
                            // 因为往队列里面放消息也需要锁，如果取不出消息时阻塞将导致无法添加消息
                            action = actionQueue.poll();

                            // 下面的小睡是必须的，防止此线程过快的取消息队列。
                            Thread.sleep(1);
                        }

                        if (aiPlaying)
                            continue;

                        if (action == LEFT) {
                            gameContainer.moveLeft();
                        } else if (action == RIGHT) {
                            gameContainer.moveRight();
                        } else if (action == TRANSFORM) {
                            gameContainer.transform();
                        } else if (action == DOWN) {
                            if (!gameContainer.moveDown()) {
                                touchBottom();
                            }
                        } else if (action == FAST_DOWN) { // 按了向下键之后，快速的下移 FAST_DOWN_CELL_COUNT 格
                            for (int i = 0; i < FAST_DOWN_CELL_COUNT; i++) {
                                if (!gameContainer.moveDown()) {
                                    touchBottom();
                                    break; // 当前小方块已经固定在底部了
                                }
                                Thread.sleep(10);
                            }
                        } else {
                            throw new NeverReachHereError();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    private static final String TITLE = "AI Tetris";

    private static final int ROW = 20;   // 行数
    private static final int COL = 10;   // 列数

    // padding 相当于 窗口高度的比例
    private static final double WINDOW_PADDING_PROPORTION = 0.006;   // 1%
    // spacing 相当于 窗口高度的比例
    private static final double WINDOW_SPACING_PROPORTION = 0.006;   // 1%

    private static final String BACKGROUND_COLOR = "0xa7b7b1";

    private static final String PAUSE = "Pause";
    private static final String START = "Start";
    private static final String RESTART = "Restart";

    private static final String AI_PLAYS = "AI plays";
    private static final String I_PLAY = "I play";

//    private double windowWidth;
    private double windowHeight;

//    private double gameWidth;
    private double gameHeight;

    @Override
    public void init() throws Exception {
        super.init();

        Rectangle2D screenRectangle = Screen.getPrimary().getBounds();
        double width = screenRectangle.getWidth();
        double height = screenRectangle.getHeight();
        System.out.println(width + ", " + height);

        windowHeight = height*2/3;
//        windowWidth = windowHeight * (COL / (double)ROW) + windowHeight/ROW*4;

        gameHeight = windowHeight * (1 - 2*WINDOW_PADDING_PROPORTION);
        double gameWidth = gameHeight * (COL / (double)ROW);

        gameContainer = new Container(gameWidth, gameHeight, COL, ROW);

        double len = gameContainer.blockSideLen*Block.SIDE_LEN + gameContainer.gapBetweenBlocks*(Block.SIDE_LEN-1);
        nextBlockContainer = new Container(len, len, Block.SIDE_LEN, Block.SIDE_LEN);

        ai = new ElTetris(gameContainer);
    }

    private Text scoreLabel;
    private Text HiScoreLabel;
    private Text levelLabel;
    private Text speedLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(TITLE);
        primaryStage.getIcons().add(new Image("file:src/main/resources/icon.png"));

        primaryStage.setResizable(false);

        VBox infoPanel = new VBox();
        infoPanel.setAlignment(Pos.TOP_RIGHT);

        infoPanel.getChildren().add(new Label("Score"));
        scoreLabel = new Text(String.valueOf(currScore));
        scoreLabel.setStyle("-fx-font-weight: bold");
        infoPanel.getChildren().add(scoreLabel);

        infoPanel.getChildren().add(new Text()); // empty text to separate

        infoPanel.getChildren().add(new Label("Hi-Score"));
        HiScoreLabel = new Text("unknown");
        HiScoreLabel.setStyle("-fx-font-weight: bold");
        infoPanel.getChildren().add(HiScoreLabel);

        infoPanel.getChildren().add(new Text()); // empty text to separate

        infoPanel.getChildren().add(new Label("Next"));
        infoPanel.getChildren().add(nextBlockContainer);

        infoPanel.getChildren().add(new Text()); // empty text to separate

        infoPanel.getChildren().add(new Label("Level"));
        levelLabel = new Text(String.valueOf(currLevel));
        levelLabel.setStyle("-fx-font-weight: bold");
        infoPanel.getChildren().add(levelLabel);

        infoPanel.getChildren().add(new Text()); // empty text to separate

        infoPanel.getChildren().add(new Label("Speed"));
        speedLabel = new Text("unknown");  // todo
        speedLabel.setStyle("-fx-font-weight: bold");
        infoPanel.getChildren().add(speedLabel);

        infoPanel.getChildren().add(new Text()); // empty text to separate

        Button pauseBtn = new Button(PAUSE);
        pauseBtn.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) { // 鼠标左键
                if (isInPause) {
                    isInPause = false;
                    pauseBtn.setText(PAUSE);
                } else {
                    isInPause = true;
                    pauseBtn.setText(START);
                }
            }
        });
        pauseBtn.setFocusTraversable(false);
        infoPanel.getChildren().add(pauseBtn);

        infoPanel.getChildren().add(new Text()); // empty text to separate

        Button aiBtn = new Button(AI_PLAYS);
        aiBtn.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) { // 鼠标左键
                if (aiPlaying) {
                    aiPlaying = false;
                    aiBtn.setText(AI_PLAYS);
                } else {
                    aiPlaying = true;
                    aiBtn.setText(I_PLAY);
                }
            }
        });
        aiBtn.setFocusTraversable(false);
        infoPanel.getChildren().add(aiBtn);

        HBox hBox = new HBox();
        hBox.setBackground(new Background(new BackgroundFill(Color.web(BACKGROUND_COLOR),null,null)));
        double padding = WINDOW_PADDING_PROPORTION * windowHeight;
        hBox.setPadding(new Insets(padding));
        hBox.setSpacing(WINDOW_SPACING_PROPORTION * windowHeight);
        hBox.getChildren().add(gameContainer);
        Line line = new Line(0,0,0, gameHeight);
        hBox.getChildren().add(line);
        hBox.getChildren().add(infoPanel);

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            try {
                if (keyEvent.getCode() == KeyCode.LEFT) {
                    actionQueue.put(LEFT);
                } else if (keyEvent.getCode() == KeyCode.RIGHT) {
                    actionQueue.put(Action.RIGHT);
                } else if (keyEvent.getCode() == KeyCode.UP) {
                    actionQueue.put(Action.TRANSFORM);
                } else if (keyEvent.getCode() == KeyCode.DOWN) {
                    actionQueue.put(Action.FAST_DOWN);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            System.out.println(keyEvent.toString()); ////////////////////////////////
        });

        primaryStage.setScene(new Scene(hBox, -1, windowHeight));
        primaryStage.show();

        gameContainer.draw();

        autoDownThread.start();
        workThread.start();
    }

    private void gameOver() {
        isGameOver = true;
        isInPause = true;
//        player.pause();
//
//        Message msg = new Message();
//        msg.what = GAME_OVER;
//        handler.sendMessage(msg);
//
        new Thread(() -> {
            try {
                // 等待各线程自动结束
                if (workThread != null) {
                    workThread.join();
                }

                if (autoDownThread != null) {
                    autoDownThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            actionQueue.clear();

//            gameContainer.overPattern();
//            gameContainer.draw();

//                nextBlockPanel.resetPanel();
//                nextBlockPanel.drawPanel();
        }).start();

        System.out.println("Game Over");
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        isGameOver = true;
        isInPause = true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
