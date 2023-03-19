package com.example.numberrecognitionapp.Drawable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

/* NOTE: The class essentially takes picture on every before starting every drawing and uses that as the new background.
If not done this way, the drawing lags after a couple of seconds since the drawing path gets too long. The path is reset
on every press which stops the lag from happening. Of course if the user drew for a long period without lifting their finger
the drawing would start to lag anyways but this is how it's done on professional drawing software as well.

This approach also allows the "undo" and "redo" functionalities to be easily implemented, which are not done here sinc this
project doesn't need it.(if one wants to implement that, the picture taking should be changed so it happens on finger lift)
*/

// Suppress all lints
@SuppressLint("all")
@SuppressWarnings("all")

public class DrawableView extends View {
    /* ->View is what holds the shape of the canvas and the canvas is what you change to change the view
    *  ->You use paint object to draw, it can be imagined as the brush
    *  ->You use path to move the brush along the path to draw(can be predetermined shape path
    * like circle or user drawn as below)
    */

    // Define the path and paint objects as class variables since they will be used by different methods
    private Path mPath;
    private Paint mPaint;
    private Paint mPaintBackground;

    // Define the bitmap that will hold the current view's bitmap(for sending to the tflite model and
    // for optimization that will be explained later)
    private Bitmap mCurrentCanvasBitmap;

    // Define location variable that will hold the current and previous touch locations(for optimization
    // that will be explained later)
    // Locations go like this: [previousX , previousY, currentX, currentY]
    float mLocation[] = new float[]{0, 0, 0, 0};

    // The custom canvas and bitmap that will go into it. This will be used to get the picture of the view
    // so we can use it to save the state of the drawing instead of making it one complete path function
    // which will lag the application. This will also allow us to make undo or redo functions.
    // (Not done in this app since it doesn't need it.)
    private Bitmap mBitmap;
    private Canvas mCanvas;

    // System generated constructors(I auto generated them by pressing alt+insert and selecting all)
    // You have to call init(null); and init(attr); to pass in attributes from the xml file created
    // by me under the "java/res/values/attr.xml". app:square_color="" can be written to change the
    // attributes being passed in here but I haven't used them because this application doesn't
    // need it. Attributes can be read from these constructors and assigned to a class variable inside
    // there.(Don't forget that the variables defined in the attr.xml are unused for this app)
    public DrawableView(Context context) {
        super(context);
        init(null);
    }
    public DrawableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }
    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }
    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    // This is the function that all of the constructors call so you can think of this function as
    // the initialization function.(Note that attribute set can be passed in but is optional(nullable))
    private void init(@Nullable AttributeSet set){

        // Create and configure the path and paint objects that will be used for drawing
        // Note that these values can be changed from the main activity by changing these
        // object variables, since the view is automatically imported you can directly access these.
        // Make sure the object you want to access is a "public" variable or you cant access them.
        // Since you don't create an instance of this class in main activity and instead the xml
        // does this for you, you have to access the variables and methods by assigning the class
        // to a varible by findviewbyid() method.("drawing" variable inside main activity is the example)
        // Example: drawing.mPaint.setColor(Color.BLUE);
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(40f);

        // Create the paint object that will make the background black(Note that the paint style is
        // "FILL" which makes this possible)
        mPaintBackground = new Paint();
        mPaintBackground.setColor(Color.BLACK);
        mPaintBackground.setStyle(Paint.Style.FILL);

        // Create a new istance of a canvas object that will be used to get the bitmap of the whole view
        mCanvas = new Canvas();
    }

    // Make an onTouchEvent to detect movement on this view
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Keep the response from the superclass so you can send it if nothing executes in the code below
        boolean value = super.onTouchEvent(event);

        // Stop the scrollview(which is the parent for this case) to scroll if the touch is within this view
        getParent().requestDisallowInterceptTouchEvent(true);

        // Read the current values of x and y coordinates user pressed
        mLocation[2] = event.getX();
        mLocation[3] = event.getY();

        //If the user just pressed down
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            //Get the current drawing as a bitmap
            mCurrentCanvasBitmap = getBitmap();
            //Reset the path(to stop the drawing from lagging)
            mPath.reset();
            //Move path back to release location for the action move to add path
            mPath.moveTo(mLocation[2], mLocation[3]);
            //Update the previous values
            mLocation[0] = mLocation[2];
            mLocation[1] = mLocation[3];
            return true;
        }
        //If the user just released their press
        else if (event.getAction() == MotionEvent.ACTION_UP){
            //Update drawing and do nothing else
            postInvalidate();
            //Update the previous values
            mLocation[0] = mLocation[2];
            mLocation[1] = mLocation[3];
            return true;
        }
        ///If the user is moving their finger
        else if (event.getAction() == MotionEvent.ACTION_MOVE){
            //Draw line to the move location
            mPath.lineTo(mLocation[2], mLocation[3]);
            //Update drawing
            postInvalidate();
            //Update the previous values
            mLocation[0] = mLocation[2];
            mLocation[1] = mLocation[3];
            return true;
        }

        // Return subclass' response if the code hasn't returned till the end.
        return value;

    }

    // Function to clear the view
    public void clearDrawing(){
        // Make the bitmap null so that "onDraw" skips drawing it and only draws the background.
        mCurrentCanvasBitmap = null;
        // Call the "onDraw" method
        postInvalidate();
    }

    // Function to get the bitmap of the current view
    public Bitmap getBitmap(){
        // Create a new bitmap with the same size as the view. Generating a bitmap over and over again
        // may seem redundant but the system automatically deletes the bitmap by garbage collection (apparently?)
        // so if you don't regenerate it the bitmap will return null and the view will be reset.
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        // Update the custom canvas' bitmap again
        mCanvas.setBitmap(mBitmap);
        // Call the "onDraw" method with the custom canvas instead of the superclass one, which is
        // connected to the bitmap we created. Apparently you cannot access the superclass' canvas' bitmap
        // directly so you have to redraw the view into your own canvas that has an accessable bitmap you
        // have passed to it.(This method directly jumps to "onDraw" unlike the "postInvalidate()" method!)
        draw(mCanvas);
        // Return the bitmap
        return mBitmap;
    }

    // This function is called when the first launch happens or a invalidation occurs
    // It can also be called using "draw(canvas);" method but it requires a custom canvas passed with it.
    // The "postInvalidate();" method or natural occurance of this method will pass the superclass' canvas
    // instead which is why you have to use "postInvalidate();" instead of directly calling it.
    @Override
    protected void onDraw(Canvas canvas) {
        // Keep the superclass code
        super.onDraw(canvas);

        // If the bitmap is not initialized or was cleared just draw the background and return
        if(mCurrentCanvasBitmap == null){
            canvas.drawPaint(mPaintBackground);
            return;
        }

        // Draw the previous bitmap
        canvas.drawBitmap(mCurrentCanvasBitmap, 0, 0, null);

        // Draw the new path
        canvas.drawPath(mPath, mPaint);
    }
}
