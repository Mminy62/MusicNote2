package com.example.musicnote;

import android.annotation.SuppressLint;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.schemas.lull.Quat;

import java.util.Timer;
import java.util.TimerTask;

public class GameNote extends Node {

    ArSceneView arSceneView; // 카메라 위치 알기 위함
    GameSystem gameSystem; // up Vector와 parnet의 위치 => SetNotePosition으로 위치 갱신
    int position; // 오른쪽 생성 노드인지 왼쪽 생성 노드인지 확인 (true => right, false => left)
    float speed; // 노트의 이동 속도
    int score;
    float distance = 0f;

    private static final String TAG = "location :";

    final float HEIGHT;
    ImageView tapButton01;
    ImageView tapButton02;
    ImageView tapButton03;
    ImageView getTapButton;
    ImageView background;

    Handler m_handler;
    Runnable m_handlerTask ;
    int timeleft = 2;



    MediaPlayer effectSound;


    GameNote(ArSceneView arSceneView, GameSystem gameSystem, ModelRenderable noteRenderable, float speed, float height, int score, int position,
             ImageView getTapButton, ImageView background){
        this.arSceneView = arSceneView;
        this.gameSystem = gameSystem;
        this.speed = speed;
        this.HEIGHT = height;
        this.score = score; // 50
        this.position = position;
      /*  this.tapButton01 = tapButton01;
        this.tapButton02 = tapButton02;
        this.tapButton03 = tapButton03;*/
        this.getTapButton = getTapButton;
        this.background = background;



        this.setRenderable(noteRenderable);

        //음표크기 (기본설정 0.5f)
        this.setLocalScale(new Vector3(0.20f, 0.20f, 0.20f));
        this.setParent(gameSystem);

        setPosition();

        //음표 회전
        Quaternion rotation1 = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), 0); // rotate X axis 90 degrees
        Quaternion rotation2 = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), 0); // rotate Y axis 90 degrees
        this.setLocalRotation(Quaternion.multiply(rotation1, rotation2));

        // this.setLocalRotation(Quaternion.axisAngle(this.getUp(), 90));
        //this.setLocalRotation(Quaternion.axisAngle(this.getDown(),0));//멈춰서 내려옴 회전 안함


        // 오브젝트 카메라 바라보게 회전
        /*
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, gameSystem.getWorldPosition());
        Quaternion direction = Quaternion.lookRotation(objToCam, gameSystem.getUp());
        this.setWorldRotation(direction);
        */

        m_handler = new Handler();

        this.setOnTapListener((v, event) ->{

            getScore(); // 터치 시 50씩 추가

            if(distance > 3.4f && distance < 4.0f){

                getTapButton.setVisibility(View.VISIBLE);
                background.setVisibility(View.VISIBLE);

                m_handlerTask = new Runnable()
                {
                    @Override
                    public void run() {
                        if(timeleft > 0)
                        {
                            // do stuff
                            Log.i("timeleft",""+timeleft);
                            timeleft--;
                        }
                        else
                        {
                            m_handler.removeCallbacks(m_handlerTask);
                            getTapButton.setVisibility(View.INVISIBLE);
                            background.setVisibility(View.INVISIBLE);
                            timeleft = 2;
                        }
                        m_handler.postDelayed(m_handlerTask, 500);
                    }
                };
                m_handlerTask.run();
            }
            else{
                getTapButton.setVisibility(View.INVISIBLE);
                background.setVisibility(View.INVISIBLE);
            }

        });
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        float deltaTime = frameTime.getDeltaSeconds();

        setPosition(deltaTime);

        // 노래가 종료될 시에도 삭제 (어차피 게임중에는 pause 못하긴 할듯 => 그래서 그냥 노래 플레이중 아니면 삭제)
        if(gameSystem.isPlaying != 2) removeNote();

        if(distance > HEIGHT * 3.5f){
            removeNote();
        }



    }


    // 위치 조정
    public void setPosition(float deltaTime){
        Camera camera = arSceneView.getScene().getCamera();
        Vector3 down = camera.getDown();
        Vector3 up = camera.getUp();
        Vector3 right = camera.getRight();

        Vector3 parentPos = Vector3.add(gameSystem.getWorldPosition(), up.scaled(HEIGHT));
        Vector3 movePos = Vector3.add(parentPos, gameSystem.SetNotePosition(position));
        movePos = Vector3.add(movePos, down.scaled(distance));

        movePos = Vector3.add(movePos, right.scaled(-0.5f));
        //상수가 증가할수록 화면 오른쪽으로 음표가 이동

        this.setWorldPosition(movePos);

            distance += deltaTime * speed;

    }

    public void setPosition(){
        Camera camera = arSceneView.getScene().getCamera();
        Vector3 down = camera.getDown();
        Vector3 up = camera.getUp();
        Vector3 right = camera.getRight();

        Vector3 parentPos = Vector3.add(gameSystem.getWorldPosition(), up.scaled(HEIGHT));
        Vector3 movePos = Vector3.add(parentPos, gameSystem.SetNotePosition(position));
        movePos = Vector3.add(movePos, down.scaled(distance));

        movePos = Vector3.add(movePos, right.scaled(-0.5f));

        this.setWorldPosition(movePos);
    }

    public void getScore(){
        //removeNote();
        gameSystem.getScore(score);// 50씩 보내기
        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {
            int count = 0;
            @Override
            public void run() {
                //if(count >= 20) removeNote();
                Quaternion rotation = Quaternion.axisAngle(getUp(), 60);
                setWorldRotation(Quaternion.multiply(rotation, getWorldRotation()));
                count++;
            }
        };

        timer.schedule(tt, 0, 50);
    }

    public void removeNote(){
        gameSystem.removeChild(this);
        this.setParent(null);
    }
}
