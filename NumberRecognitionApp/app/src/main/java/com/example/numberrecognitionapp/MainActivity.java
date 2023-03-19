package com.example.numberrecognitionapp;

/* DETAILED EXPLANATION:
-------------------------------------------------------------------------------------------------
1-) To load you model you have to create an assets folder which you can do by
right clicking the app "new/folder/assets_folder" this will automatically add the assets folder
to the right place.
-------------------------------------------------------------------------------------------------
2-) After that put your model into the assets folder created.
-------------------------------------------------------------------------------------------------
3-) Open "build.gradle(Module: app)" inside of the android add this:

android {
    .....
    aaptOptions{
        noCompress "tflite"
    }
}
-------------------------------------------------------------------------------------------------
4-) In the "build.gradle(Module: app)" inside of the dependencies add this:

dependencies {
    .....
    implementation 'org.tensorflow:tensorflow-lite:+'
}
-------------------------------------------------------------------------------------------------
5-) In the main activity add:

import org.tensorflow.lite.Interpreter;
-------------------------------------------------------------------------------------------------
6-) In the "onCreate" initialize the model by adding:

 tflite = new Interpreter(loadModelFile());
-------------------------------------------------------------------------------------------------
7-) Define the loadModelFile() method by copy and pasting(only need to change the model's name if
it's different):

private MappedByteBuffer loadModelFile() throws IOException{
    AssetFileDescriptor fileDescriptor = this.getAssets().openFd("linear.tflite");
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
}
-------------------------------------------------------------------------------------------------

TO USE THE MODEL:
Use the "tflite.run(input, output);" it is a setter method so the output will be changed and you
don't need to assign a returned variable for it. Both the input and the output have to be float
arrays and the output will be 2 dimension array with the first dimension being only one long and
the second dimension being output neurons' length.
-------------------------------------------------------------------------------------------------
Note: It doesn't matter for this app since it's so simple, so I didn't implement it but make sure
the model works on a different thread since it will almost always take long enough that it will
lag the main thread, this is especially true for anything remotely complicated. You can return
the output as an object to the event handler which can update the ui.
*/

import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.numberrecognitionapp.Drawable.DrawableView;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

// Suppress all lints
@SuppressLint("all")
@SuppressWarnings("all")

public class MainActivity extends AppCompatActivity {

    // Create the UI objects
    private DrawableView drawing;
    private Button clearBtn;
    private TextView probabilitiesText;
    private Button guessBtn;

    // Create the bitmap holders for the drawable
    private Bitmap drawing_bitmap;
    private Bitmap scaled_bitmap;

    // The interpreter that will be the middleman between the ai model and the app
    private Interpreter tflite;

    // Create other variables
    private int pixels[] = new int[784];
    private float gray_pixels[] = new float[784];
    private float output[][] = new float[1][10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the id's from the ui and assign them to objects inside of the code
        drawing = findViewById(R.id.drawing);
        clearBtn = findViewById(R.id.clearBtn);
        probabilitiesText = findViewById(R.id.probabilitiesText);
        guessBtn = findViewById(R.id.guessBtn);

        // Initialize the interpreter for the ai model
        try{
            tflite = new Interpreter(loadModelFile());
        }catch (Exception ex){
            // Deliberately crash the app if the model doesn't load(you have to do this since
            // the android studio doesn't allow you to do the above code without try catch block)
            int x = 5/0;
        }

        //region -On click listener for the "clear" button
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the drawing
                drawing.clearDrawing();
                // Clear the probabilities
                probabilitiesText.setText(" Number 0:" +
                                        "\n Number 1:" +
                                        "\n Number 2:" +
                                        "\n Number 3:" +
                                        "\n Number 4:" +
                                        "\n Number 5:" +
                                        "\n Number 6:" +
                                        "\n Number 7:" +
                                        "\n Number 8:" +
                                        "\n Number 9:" +
                                        "\n\n Final Guess: ...");
            }
        });
        //endregion

        //region -On click listener for the "guess" button
        guessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the bitmap of the current drawing
                drawing_bitmap = drawing.getBitmap();
                //Scale the bitmap to what the nn can read(28x28 gray scale for this model)
                scaled_bitmap = Bitmap.createScaledBitmap(drawing_bitmap, 28, 28,false);
                // Get the pixel array from the scaled image
                scaled_bitmap.getPixels(pixels, 0, 28,0,0, 28, 28);

                //Extract the rgb values from the bitmap and apply the formula to change it to grayscale
                int R,G,B;
                for(int i=0; i<784; i++){
                    R = (pixels[i] >> 16) & 0xff;
                    G = (pixels[i] >> 16) & 0xff;
                    B = (pixels[i] >> 16) & 0xff;
                    gray_pixels[i] = ( (0.3f * R)+(0.59f * G)+(0.11f * B) )/255f;
                }

                // Run the model with the bitmap and get the output
                tflite.run(gray_pixels, output);

                // Get the highest guess value and find it's index
                float max = output[0][0];
                int guess = 0;
                for(int i=1; i<10; i++){
                    if (max > output[0][i]){
                    }
                    else{
                        max = output[0][i];
                        guess = i;
                    }
                }

                // Give the user every guess and the best guess
                probabilitiesText.setText(" Number 0: " + (int)(output[0][0]*100f) + "%" +
                        "\n Number 1: " + (int)(output[0][1]*100f) + "%" +
                        "\n Number 2: " + (int)(output[0][2]*100f) + "%" +
                        "\n Number 3: " + (int)(output[0][3]*100f) + "%" +
                        "\n Number 4: " + (int)(output[0][4]*100f) + "%" +
                        "\n Number 5: " + (int)(output[0][5]*100f) + "%" +
                        "\n Number 6: " + (int)(output[0][6]*100f) + "%" +
                        "\n Number 7: " + (int)(output[0][7]*100f) + "%" +
                        "\n Number 8: " + (int)(output[0][8]*100f) + "%" +
                        "\n Number 9: " + (int)(output[0][9]*100f) + "%" +
                        "\n\n Final Guess: " + guess);


            }
        });
        //endregion

    }


    // Copy paste code from the tensorflow to load the model.
    private MappedByteBuffer loadModelFile() throws IOException{
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("linear.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    }