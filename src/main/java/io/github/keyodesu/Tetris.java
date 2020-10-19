package io.github.keyodesu;

import io.github.keyodesu.ai.AI;
import io.github.keyodesu.ai.ElTetris;
import io.github.keyodesu.block.Block;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Line;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.concurrent.LinkedBlockingQueue;

import static io.github.keyodesu.Config.*;

/**
 * @author Yo Ka
 */
public class Tetris extends Application {

    private void reset(Canvas canvas, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private double windowWidth;
    private double windowHeight;

    private double gameWidth;
    private double gameHeight;

    private boolean isGameOver = false; // 一局游戏结束
    private boolean isInPause = false; // 游戏是否在暂停中
    private boolean isMute = false;  // 是否静音
//    private MediaPlayer player; // 用来播放背景音乐

    private Container gameContainer;
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
//                        Log.e(TAG, "错误，不应该走到这里。currLeven = " + currLevel);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    /*
     * 工作线程
     * 从消息队列中取消息，然后处理消息。
     */
    Thread workThread = new Thread(new Runnable() {
        private static final int BEGIN_X = 3;  // 默认小方块从第3列出来
        private static final int BEGIN_Y = -1;

        // 当前block的位置
        private int x = BEGIN_X;
        private int y = BEGIN_Y;

        private Block currBlock = Block.getRandomBlock();
        private Block nextBlock = Block.getRandomBlock();

        /**
         * 触底后的处理
         */
        private void touchBottom() {
            gameContainer.solidifyAllBlocksMovingStat();

            int removeLineCount = gameContainer.removeFullLines();
            if(removeLineCount > 0) {
//                Log.i(TAG, "消除了" + removeLineCount + "行");
                gameContainer.draw();

                if (removeLineCount == 1)
                    currScore += 10;   //  一次消除一层： 获得10积分
                else if (removeLineCount == 2)
                    currScore += 30;   //  一次消除二层： 获得30积分
                else if (removeLineCount == 3)
                    currScore += 60;   //  一次消除三层： 获得60积分
                else if (removeLineCount == 4)
                    currScore += 100;  //  一次消除四层： 获得100积分
                else
//                    Log.e(TAG, "错误，不应该走到这里！消除了" + removeLineCount + "行");

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

//                Message msg = new Message();
//                msg.what = GAME_INFO_CHANGE;
//                handler.sendMessage(msg);
            }

            currBlock = nextBlock;
            nextBlock = Block.getRandomBlock();
//            nextBlockPanel.setBlockStat(0, 0, 4, 4, nextBlock.getData());
//            nextBlockPanel.drawPanel();

            if(y == 0) {  // 往第0行放时已经触底了，说明游戏结束了。
                gameOver();
            } else {
                // 还原一下状态
                x = BEGIN_X;
                y = BEGIN_Y;
                // 清空一下消息队列，不要让对上次小方块的指令影响到新的小方块
                actionQueue.clear();
            }
        }

        /**
         * 进行一次移动或变形
         * @param action
         * @return 返回当前小方块在一次移动后是否已经固定在底部了。
         *          true表示已经固定，false表示没有固定
         */
        private boolean move(Action action) {
            if(!gameContainer.testConflict(x, y - 3, currBlock)) {  // 没有碰到任何边界
                gameContainer.cleanAllBlocksMovingStat();  //清理block运动中中上一次的残留
                gameContainer.pasteBlock(x, y - 3, currBlock);
                gameContainer.draw();
            } else {
                // 有冲突，还原一下状态
                if(action == Action.LEFT) {
                    x++;
                } else if(action == Action.RIGHT) {
                    x--;
                } else if(action == Action.DOWN || action == Action.FAST_DOWN) {
                    // 往下运动有冲突，说明触底了
                    touchBottom();
                    return true;
                } else if(action == Action.TRANSFORM) {
                    currBlock.switchToPrevStat();
                } else {
//                    Log.e(TAG, "error, 不应该走到这里，检测碰撞出错！");
                }
            }

            return false;
        }

        @Override
        public void run() {
            while (!isGameOver) {
                try {
                    if (aiPlaying) {
                        x = ai.calBestColAndStat(currBlock);
                        while (!move(Action.DOWN)) {
                            y++;
                            Thread.sleep(10);
                        }
                    } else {
//                    nextBlockPanel.setBlockStat(0, 0, 4, 4, nextBlock.getData());  todo
//                    nextBlockPanel.drawPanel();

                        Action action = null;

                        while (action == null && !aiPlaying) {
                            if (isGameOver) {
                                return;
                            }
                            /*
                             * 循环取消息直到取出消息为止 注意，取消息不能用阻塞函数actionQueue.take()
                             * 因为往队列里面放消息也需要锁，如果取不出消息时阻塞将导致无法添加消息
                             */
                            action = actionQueue.poll();

                            // 下面的小睡是必须的，防止此线程过快的取消息队列。
                            Thread.sleep(1);
                        }

                        if (aiPlaying)
                            continue;

                        switch (action) {
                        case LEFT:
                            x--;
                            move(action);
                            break;
                        case RIGHT:
                            x++;
                            move(action);
                            break;
                        case TRANSFORM:
                            currBlock.switchToNextStat();
                            move(action);
                            break;
                        case DOWN:
                            y++;
                            move(action);
                            break;
                        case FAST_DOWN: // 按了向下键之后，快速的下移 FAST_DOWN_CELL_COUNT 格
                            for (int i = 0; i < FAST_DOWN_CELL_COUNT; i++) {
                                y++;
                                if (move(action)) {
                                    break; // 当前小方块已经固定在底部了
                                }
                                Thread.sleep(10);
                            }
                            break;
                        default:
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    @Override
    public void init() throws Exception {
        super.init();

        Rectangle2D screenRectangle = Screen.getPrimary().getBounds();
        double width = screenRectangle.getWidth();
        double height = screenRectangle.getHeight();
        System.out.println(width + ", " + height);

        windowHeight = height*2/3;
        windowWidth = windowHeight * (COL / (double)ROW) + windowHeight/ROW*4;

        gameHeight = windowHeight * (1 - 2*GAP_AROUND_PROPORTION);
        gameWidth = gameHeight * (COL / (double)ROW);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(TITLE);

        gameContainer = new Container(gameWidth, gameHeight);

        ai = new ElTetris(gameContainer);

        VBox infoPanel = new VBox();
        infoPanel.getChildren().add(new Label("Score"));
        infoPanel.getChildren().add(new Label("200"));
        infoPanel.getChildren().add(new Label("Next"));
        infoPanel.getChildren().add(new Label("Level"));
        infoPanel.getChildren().add(new Label("3"));
        infoPanel.getChildren().add(new Label("Speed"));

        Button btn = new Button(AI_PLAYS);
        btn.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) { // 鼠标左键
                if (aiPlaying) {
                    aiPlaying = false;
                    btn.setText(AI_PLAYS);
                } else {
                    aiPlaying = true;
                    btn.setText(I_PLAY);
                }
            }
        });
        btn.setFocusTraversable(false);
        infoPanel.getChildren().add(btn);

        HBox hBox = new HBox();
        hBox.setBackground(new Background(new BackgroundFill(Color.web(BACKGROUND_COLOR),null,null)));
        double padding = GAP_AROUND_PROPORTION * windowWidth;
        hBox.setPadding(new Insets(padding));
        hBox.setSpacing(4);
        hBox.getChildren().add(gameContainer);
        Line line = new Line(0,0,0, gameHeight);
        hBox.getChildren().add(line);
        hBox.getChildren().add(infoPanel);

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            try {
                if (keyEvent.getCode() == KeyCode.LEFT) {
                    actionQueue.put(Action.LEFT);
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

        primaryStage.setScene(new Scene(hBox, windowWidth, windowHeight));
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

            gameContainer.overPattern();
            gameContainer.draw();

//                nextBlockPanel.resetPanel();
//                nextBlockPanel.drawPanel();
        }).start();
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
