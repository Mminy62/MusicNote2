package com.example.musicnote;

import android.util.Log;

import com.google.android.filament.View;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;



public class PointHand extends Node {

    private ArSceneView arSceneView;
    private AnchorNode parent;

    PointHand(AnchorNode parent, ModelRenderable handModel, ArSceneView arSceneView) {

        this.setParent(parent);
        this.parent = parent;
        this.setRenderable(handModel);
        this.arSceneView = arSceneView;

        this.setLocalScale(new Vector3(0.4f, 0.4f, 0.4f));
        this.setLocalPosition(this.getUp().scaled(2f));

        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Vector3 up = this.getUp();

        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();


        Vector3 v = Vector3.subtract(cameraPos, this.getWorldPosition());
        float distance = (float) Math.sqrt(Vector3.dot(v, v));

        // 사용자와 거리가 50m이상 벌어지면 삭제
        if (distance > 50) {
            parent.removeChild(this);
            this.setParent(null);
            arSceneView.getScene().removeChild(parent);
            parent.getAnchor().detach();
            parent.setParent(null);
            parent = null;
            Log.i("PointHand", "object is removed");
        }
    }

    public void removeNode(){
        parent.removeChild(this);
        this.setParent(null);
        arSceneView.getScene().removeChild(parent);
        parent.getAnchor().detach();
        parent.setParent(null);
        parent = null;
        Log.i("PointHand", "object is removed");

    }
}
