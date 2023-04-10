package org.vitinho.mesh3d;

import java.io.FileOutputStream;

public class GenerateSquareStl{
    public static void main(String[] args) throws Exception{
        GenerateSquareStl solid = new GenerateSquareStl();
    }

    public GenerateSquareStl() throws Exception{
        FileOutputStream fos = new FileOutputStream("square.stl");
        for(int i = 0; i < 80; i++){
            fos.write(0);
        }
        int numberOfFaces = 12;
        writeData(fos,numberOfFaces);
        writeTriangle(fos, new float[]{0.0f,0.0f,0.0f}, new float[]{1.0f,0.0f,0.0f}, new float[]{0.0f,1.0f,0.0f});
        writeTriangle(fos, new float[]{1.0f,1.0f,0.0f}, new float[]{1.0f,0.0f,0.0f}, new float[]{0.0f,1.0f,0.0f});
        writeTriangle(fos, new float[]{0.0f,0.0f,1.0f}, new float[]{1.0f,0.0f,1.0f}, new float[]{0.0f,1.0f,1.0f});
        writeTriangle(fos, new float[]{1.0f,1.0f,1.0f}, new float[]{1.0f,0.0f,1.0f}, new float[]{0.0f,1.0f,1.0f});
        
        writeTriangle(fos, new float[]{0.0f,0.0f,0.0f}, new float[]{1.0f,0.0f,0.0f}, new float[]{0.0f,0.0f,1.0f});
        writeTriangle(fos, new float[]{1.0f,0.0f,1.0f}, new float[]{1.0f,0.0f,0.0f}, new float[]{0.0f,0.0f,1.0f});
        writeTriangle(fos, new float[]{0.0f,1.0f,0.0f}, new float[]{1.0f,1.0f,0.0f}, new float[]{0.0f,1.0f,1.0f});
        writeTriangle(fos, new float[]{1.0f,1.0f,1.0f}, new float[]{1.0f,1.0f,0.0f}, new float[]{0.0f,1.0f,1.0f});
        
        writeTriangle(fos, new float[]{0.0f,0.0f,0.0f}, new float[]{0.0f,0.0f,1.0f}, new float[]{0.0f,1.0f,0.0f});
        writeTriangle(fos, new float[]{0.0f,1.0f,1.0f}, new float[]{0.0f,0.0f,1.0f}, new float[]{0.0f,1.0f,0.0f});
        writeTriangle(fos, new float[]{1.0f,0.0f,0.0f}, new float[]{1.0f,0.0f,1.0f}, new float[]{1.0f,1.0f,0.0f});
        writeTriangle(fos, new float[]{1.0f,1.0f,1.0f}, new float[]{1.0f,0.0f,1.0f}, new float[]{1.0f,1.0f,0.0f});
        fos.close();
    }
    private void writeTriangle(FileOutputStream fos,float[] point1,float[] point2,float[] point3) throws Exception{
        writeData(fos,0);
        writeData(fos,0);
        writeData(fos,1);
        writeData(fos,Float.floatToRawIntBits(100.0f*point1[0]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point1[1]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point1[2]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point2[0]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point2[1]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point2[2]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point3[0]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point3[1]));
        writeData(fos,Float.floatToRawIntBits(100.0f*point3[2]));
        fos.write(0);
        fos.write(0);
    }
    private void writeData(FileOutputStream fos, int data) throws Exception{
        System.out.println("\n" + data);
        for(int i = 0; i < 4; i++){
            fos.write((data>>(8*i))&0b11111111);
            //System.out.print(Integer.toBinaryString((data>>(8*i))^255));
            System.out.print((data>>(8*i))&0b11111111);
        }
        System.out.println("");
    }
}