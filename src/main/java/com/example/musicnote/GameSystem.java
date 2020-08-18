package com.example.musicnote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.ssomai.android.scalablelayout.ScalableLayout;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

// 리듬 노드를 생성하는 GameSystem(Anchor node)
public class GameSystem extends AnchorNode {
    class NoteCreateTimer{
        float speed; // 노트의 움직이는 속도
        int score; // 노트 터치 성공당 점수
        int[] leftTimer; // 왼쪽에서 생성되는 노트의 생성 타이밍
        int[] middleTimer;
        int[] rightTimer; // 오른쪽에서 생성되는 노트의 생성 타이밍

        NoteCreateTimer(int[][] timer, float speed, int score){
            this.leftTimer = timer[0];
            this.middleTimer = timer[1];
            this.rightTimer = timer[2];
            this.speed = speed;
            this.score = score;
        }

        public void setScore(int score){
            this.score = score;
        }

        public void setSpeed(float speed){
            this.speed = speed;
        }

        public void setTimer(int[][] timer){
            this.leftTimer = timer[0];
            this.middleTimer = timer[1];
            this.rightTimer = timer[2];
        }

        public int getLeftTimer(int index){
            return leftTimer[index];
        }

        public int getMiddleTimer(int index){ return middleTimer[index]; }

        public int getRightTimer(int index){
            return rightTimer[index];
        }

        public int getLeftLength(){
            return leftTimer.length;
        }

        public int getMiddleLength(){
            return middleTimer.length;
        }

        public int getRightLength(){
            return rightTimer.length;
        }
    }

    ArSceneView arSceneView;

    int isPlaying = 0; // 게임이 진행 중인지

    MediaPlayer currentMediaPlayer; // 현재 흘러나오는 음악(터치한 오브젝트의 음악),
    // 노래 시간에 맞추기 위해서 필요? (아니면 그냥 일정 시간 마다 진행하는걸로 => 노래가 꺼져도 진행가능)
    int musicIndex = 0;
    MusicUi musicUi; // 현재 나오는 음악 정보(playing중인지) 및 index를 알기 위해

    int LeftTimerIndex = 0; // 왼쪽 노트 타이머 index
    int MiddleTimerIndex = 0;
    int RightTimerIndex = 0; // 오른쪽 노트 타이머 index
    NoteCreateTimer musicCreater = null;

    public int currentScore = 0; // 현재까지 얻은 점수

    final float DISTANCE = 3f; // 3m (얼마나 앞에서 생성되게 할 것인지)
    final int DELAY = 2000; // 생성되고 퍼펙트 존(터치시 점수를 얻는 구역)까지 오는 데 걸리는 시간 (ms)
    final float HEIGHT = 2f; // 2m
    final float SPEED = HEIGHT * 2.5f * 1000 / DELAY; // 노트의 이동 속도(m/s)

    final int SCORE = 50;

    final float INTERVAL = 0.60f; // 0.75m

    final TextView textView;
    final TextView textView2;
    final TextView textView3;

    // UI--이민
     ImageView lineImage01;
     ImageView lineImage02;
     ImageView tapButton01;
     ImageView tapButton02;
     ImageView tapButton03;
     ImageView getTapButton01;
     ImageView getTapButton02;
     ImageView getTapButton03;
     ImageView background01;
     ImageView background02;
     ImageView background03;


     SoundPool soundPool;
     int effectSoundID;
    String scoreString2;



    ModelRenderable noteRenderable;
    ModelRenderable albumRenderable;
    Context context;

    GameSystem gameSystem;
    PopupActivity2 popupActivity2;



    // 곡 노트 타이밍 (왼쪽, 오른쪽) (ms) => [곡 인덱스][왼쪽, 오른쪽][노트 index] = 타이머
    final int[][][] NOTETIMER = {
            // 0번째 곡

            {{ 1000, 3000, 5000, 7000, 9000, 10000, 11000, 13000, 15000, 17000, 19000,
              20000, 21000, 23000, 25000, 27000, 29000, 30000, 31000, 33000, 35000, 37000, 39000,
              40000, 41000, 43000, 45000, 47000, 49000, 50000, 51000, 53000, 55000, 57000, 59000,
              60000, 61000, 63000, 65000, 67000, 69000, 70000, 71000, 73000, 75000, 77000, 79000,
              80000, 81000, 83000, 85000, 87000, 89000, 90000, 91000, 93000, 95000, 97000, 99000,
              100000, 101000, 103000, 105000, 107000, 109000, 110000, 111000, 113000, 115000, 117000, 119000,
              120000}, // 왼쪽 노트
             { 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000,
              50000, 55000, 60000, 65000, 70000, 75000, 80000, 85000, 90000, 95000,
              100000, 105000, 110000, 115000, 120000}, // 중간 노트
             {2000, 4000, 6000, 8000, 10000, 12000, 14000, 16000, 18000, 20000,
              22000, 24000, 26000, 28000, 30000, 32000, 34000, 36000, 38000, 40000,
              42000, 44000, 46000, 48000, 50000, 52000, 54000, 56000, 58000, 60000,
              62000, 64000, 66000, 68000, 70000, 72000, 74000, 76000, 78000, 80000,
              82000, 84000, 86000, 88000, 90000, 92000, 94000, 96000, 98000, 100000,
              102000, 104000, 106000, 108000, 110000, 112000, 114000, 116000, 118000, 120000}}, // 오른쪽 노트

            // 1번째 곡
            {{0000, 1000, 3000, 5000, 7000, 9000, 10000, 11000, 13000, 15000, 17000, 19000,
              20000, 21000, 23000, 25000, 27000, 29000, 30000, 31000, 33000, 35000, 37000, 39000,
              40000, 41000, 43000, 45000, 47000, 49000, 50000, 51000, 53000, 55000, 57000, 59000,
              60000, 61000, 63000, 65000, 67000, 69000, 70000, 71000, 73000, 75000, 77000, 79000,
              80000, 81000, 83000, 85000, 87000, 89000, 90000, 91000, 93000, 95000, 97000, 99000,
              100000, 101000, 103000, 105000, 107000, 109000, 110000, 111000, 113000, 115000, 117000, 119000,
              120000}, // 왼쪽 노트
             {00000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000,
              50000, 55000, 60000, 65000, 70000, 75000, 80000, 85000, 90000, 95000,
              100000, 105000, 110000, 115000, 120000}, // 중간 노트
             {0000, 2000, 4000, 6000, 8000, 10000, 12000, 14000, 16000, 18000, 20000,
              22000, 24000, 26000, 28000, 30000, 32000, 34000, 36000, 38000, 40000,
              42000, 44000, 46000, 48000, 50000, 52000, 54000, 56000, 58000, 60000,
              62000, 64000, 66000, 68000, 70000, 72000, 74000, 76000, 78000, 80000,
              82000, 84000, 86000, 88000, 90000, 92000, 94000, 96000, 98000, 100000,
              102000, 104000, 106000, 108000, 110000, 112000, 114000, 116000, 118000, 120000}}, // 오른쪽 노트

            // 2번째 곡
            {{0000, 1000, 3000, 5000, 7000, 9000, 10000, 11000, 13000, 15000, 17000, 19000,
              20000, 21000, 23000, 25000, 27000, 29000, 30000, 31000, 33000, 35000, 37000, 39000,
              40000, 41000, 43000, 45000, 47000, 49000, 50000, 51000, 53000, 55000, 57000, 59000,
              60000, 61000, 63000, 65000, 67000, 69000, 70000, 71000, 73000, 75000, 77000, 79000,
              80000, 81000, 83000, 85000, 87000, 89000, 90000, 91000, 93000, 95000, 97000, 99000,
              100000, 101000, 103000, 105000, 107000, 109000, 110000, 111000, 113000, 115000, 117000, 119000,
              120000}, // 왼쪽 노트
             {00000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000,
              50000, 55000, 60000, 65000, 70000, 75000, 80000, 85000, 90000, 95000,
              100000, 105000, 110000, 115000, 120000}, // 중간 노트
             {0000, 2000, 4000, 6000, 8000, 10000, 12000, 14000, 16000, 18000, 20000,
              22000, 24000, 26000, 28000, 30000, 32000, 34000, 36000, 38000, 40000,
              42000, 44000, 46000, 48000, 50000, 52000, 54000, 56000, 58000, 60000,
              62000, 64000, 66000, 68000, 70000, 72000, 74000, 76000, 78000, 80000,
              82000, 84000, 86000, 88000, 90000, 92000, 94000, 96000, 98000, 100000,
              102000, 104000, 106000, 108000, 110000, 112000, 114000, 116000, 118000, 120000}}, // 오른쪽 노트
    };

    GameSystem(Context context, ArSceneView arSceneView, MusicUi musicUi, TextView textView, TextView textView2,TextView textView3, ImageView lineImage01, ImageView lineImage02,
               ImageView tapButton01,ImageView tapButton02, ImageView tapButton03,
               ImageView getTapButton01, ImageView getTapButton02, ImageView getTapButton03,
               ImageView background01, ImageView background02, ImageView background03){
        // Setting
        this.context = context;
        this.arSceneView = arSceneView;
        this.musicUi = musicUi;
        this.textView = textView;
       //변경한부분-이민영
        this.textView2 = textView2; // scoreBar
        this.textView3 = textView3; // finalScore -> popup
        this.lineImage01 = lineImage01;
        this.lineImage02 = lineImage02;
        this.tapButton01 = tapButton01;
        this.tapButton02 = tapButton02;
        this.tapButton03 = tapButton03;
        this.getTapButton01 = getTapButton01;
        this.getTapButton02 = getTapButton02;
        this.getTapButton03 = getTapButton03;
        this.background01 = background01;
        this.background02 = background02;
        this.background03 = background03;

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
        effectSoundID = soundPool.load(context, R.raw.effect_sound, 1);

        // Create an ARCore Anchor at the position.

        this.setParent(arSceneView.getScene());

        setUpModel();
        SetPosition();

        // 아래 내용: 이래야만 onUpdate작동하는지 확인
        //this.setRenderable(albumRenderable);
        this.setLocalScale(new Vector3(1f, 1f , 1f));

        // 오브젝트 카메라 바라보게 회전
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Vector3 up = arSceneView.getScene().getCamera().getUp();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);

    }


    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);


        SetPosition(); // Game System의 위치를 핸드폰 앞으로 잡기

        if (isPlaying == 2){

            if(currentMediaPlayer == null){
                Log.e("ERROR: ", "currentMediaPlayer is null");
                return;
            }


            // 왼쪽 노트 타이밍 계산
            if(LeftTimerIndex < musicCreater.getLeftLength() && musicCreater.getLeftTimer(LeftTimerIndex) <= currentMediaPlayer.getCurrentPosition()  ){
              // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, noteRenderable, SPEED, HEIGHT, SCORE, 0,
                        getTapButton01, background01);

                LeftTimerIndex++;
            }

            // 중간 노트 타이밍 계산
            if(MiddleTimerIndex < musicCreater.getMiddleLength() && musicCreater.getMiddleTimer(MiddleTimerIndex) <= currentMediaPlayer.getCurrentPosition() ){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, noteRenderable, SPEED, HEIGHT, SCORE, 1,
                        getTapButton02, background02);

                MiddleTimerIndex++;
            }

            // 오른쪽 노트 타이밍 계산
            if(RightTimerIndex < musicCreater.getRightLength() && musicCreater.getRightTimer(RightTimerIndex) <= currentMediaPlayer.getCurrentPosition()){
                // GameNote 생성
                GameNote note = new GameNote(arSceneView, this, noteRenderable, SPEED, HEIGHT, SCORE, 2,
                        getTapButton03, background03);

                RightTimerIndex++;
            }
        }
    }


    // 노드 생성 시작 (일정 시간 뒤에 생성되는 걸로)
    @SuppressLint("ClickableViewAccessibility")
    public void GameStart(){
        //보이게 변경 --이민영
        lineImage01.setVisibility(View.VISIBLE);
        lineImage02.setVisibility(View.VISIBLE);
        tapButton01.setVisibility(View.VISIBLE);
        tapButton02.setVisibility(View.VISIBLE);
        tapButton03.setVisibility(View.VISIBLE);


   /*  tapButton01.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        getTapButton01.setVisibility(View.VISIBLE);
                        //tapButton01.setImageResource(R.drawable.ic_tap_btn02);
                    }
                    case MotionEvent.ACTION_UP:{
                        getTapButton01.setVisibility(View.INVISIBLE);
                        //tapButton01.setImageResource(R.drawable.ic_tap_btn);
                        break;
                    }
                }
                return false;
            }
        });

*/

   /*
   * isPlaying == 0 -> 게임 아예 끄기
   *           == 1 -> 멈췄다가 다시 시작할 경우 (멈춰진 상태)
   *           == 2 -> 게임 진행중
   * */

        if (isPlaying != 1) {
            currentMediaPlayer = musicUi.getCurrentMediaPlayer();

            if(currentMediaPlayer == null){
                Log.e("ERROR: ", "currentMediaPlayer is null");
                return;
            }

            musicIndex = musicUi.getCurrentMediaPlayerIndex();
            LeftTimerIndex = 0;
            MiddleTimerIndex = 0;
            RightTimerIndex = 0;
            musicCreater = new NoteCreateTimer(NOTETIMER[musicIndex], SPEED, SCORE);
            currentScore = 0;
        }

        SetPosition();

        isPlaying = 2;
        Log.i("DEBUG: ", "isPlaying: "+ isPlaying);
    }

    // 게임 정지
    public void GameStop(){

        isPlaying = 0;
        LeftTimerIndex = 0;
        MiddleTimerIndex = 0;
        RightTimerIndex = 0;
        currentScore = 0;
        musicCreater = null;

    }

    // 게임 일시 정지
    public void GamePause(){
        isPlaying = 1;

        lineImage01.setVisibility(View.INVISIBLE);
        lineImage02.setVisibility(View.INVISIBLE);
        tapButton01.setVisibility(View.INVISIBLE);
        tapButton02.setVisibility(View.INVISIBLE);
        tapButton03.setVisibility(View.INVISIBLE);
        getTapButton01.setVisibility(View.INVISIBLE);
        getTapButton02.setVisibility(View.INVISIBLE);
        getTapButton03.setVisibility(View.INVISIBLE);
        background01.setVisibility(View.INVISIBLE);
        background02.setVisibility(View.INVISIBLE);
        background03.setVisibility(View.INVISIBLE);


    }

    // Game System(this)의 위치 조정
    public void SetPosition(){
        Camera camera = arSceneView.getScene().getCamera();
        Vector3 cameraPos = camera.getWorldPosition(); // 카메라 위치 받아옴
        Vector3 forward = camera.getForward(); // 핸드폰 앞 벡터 받아옴

        Vector3 up = camera.getUp();
        /*
        // up vector를 법선벡터로 갖는 평면에 forward Vector 정사영구하기
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 systemPos = Vector3.subtract(forward, upValue).normalized().scaled(DISTANCE);
         */

        Vector3 position = Vector3.add(cameraPos, forward.scaled(DISTANCE));

        this.setWorldPosition(position); // 위치 설정

        // 오브젝트 카메라 바라보게 회전
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);

        //position = Vector3.add(position, up.scaled(HEIGHT));

        //position = Vector3.add(position, this.getRight().scaled(0.2275f));
        //this.setWorldPosition(position); // 위치 설정
    }

    // 왼쪽 노트와 오른쪽 노트의 생성 위치를 조정하여 반환 (0: 왼쪽, 1: 오른쪽)
    public Vector3 SetNotePosition(int position){
        Camera camera = arSceneView.getScene().getCamera();

        if(position == 0){ // 왼쪽
            return camera.getLeft().scaled(INTERVAL);
        }
        else if(position == 1){ // 중간
            return Vector3.zero();
        }
        else{ // 오른쪽
            return camera.getRight().scaled(INTERVAL);
        }

    }

    public void getScore(int score){ // 50씩 parameter

        int colorWhite = context.getResources().getColor(R.color.colorWhite);
        currentScore += score;

        String scoreString = "스코어 " + currentScore + " 점";
        int length = scoreString.length();
        SpannableStringBuilder spannable = new SpannableStringBuilder(scoreString);
        spannable.setSpan(new AbsoluteSizeSpan(45),0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(60),4, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(colorWhite),3+1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


        textView2.setText(spannable, TextView.BufferType.EDITABLE);
        textView.setText(Integer.toString(currentScore));


        soundPool.play(effectSoundID, 1f, 1f, 0, 0, 1.2f);
    }


    public int getDELAY(){
        return DELAY;
    }

    public void setUpModel(){
        ModelRenderable.builder()
                .setSource(context,R.raw.orange_note)
               // .setSource(context, R.raw.musicnote)
                .build().thenAccept(renderable -> noteRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(context, R.raw.boflogo)
                .build().thenAccept(renderable -> albumRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
    }

}
