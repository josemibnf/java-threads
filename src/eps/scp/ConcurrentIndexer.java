


import com.google.common.collect.HashMultimap;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ConcurrentIndexer implements Runnable {
    long start;
    int keySize;
    volatile String InputFilePath;
    long Limit;
    private HashMultimap<String, Long> localHash = HashMultimap.create();

    public ConcurrentIndexer(int inicio, int finnal, int keySize, String inputFilePath,int fileSize) {
        int ti;
        int tf;
        //mirar si es pot anar endavant;
        tf = (finnal+keySize);
        ti = inicio;
        if((finnal+keySize)>fileSize){
            //encas que anant endavant es superi la mida del fitxer mirem de tirar enrere
            ti= (inicio-keySize);

            tf=finnal;
        }
        this.start = ti;
        this.Limit = tf;
        //System.out.println("ThreadRang ["+start+","+Limit+")");
        //System.out.println("RUNTIME");
        //this.start = inicio; //((meitat_pos-extra+1)<0)? 0 : meitat_pos-extra+1;
        //this.Limit = finnal;//((meitat_pos+extra+1)>fileSize) ? fileSize:meitat_pos+extra;


        this.keySize=keySize;
        this.InputFilePath =inputFilePath;

        //System.out.println("Rango ["+this.start+","+this.Limit+")");

    }

    @Override
    public void run() {
        //Variables
        int initial_pos = (int) this.start;
        int car;
        String key = "";

        //Open File
        try{
            RandomAccessFile f = new RandomAccessFile(this.InputFilePath,"r");
            if(initial_pos<0){
                //en assegurem que si hi ha una posició negativa aquest sigui 0
                f.seek(0);
            }else{
                f.seek(initial_pos);
            }

            for (int pos = initial_pos; pos < this.Limit && (car = f.read())!=-1; pos++) {
                //pos++;
                if (car=='\n' || car=='\r' || car=='\t') {
                    // Sustituimos los carácteres de \n,\r,\t en la clave por un espacio en blanco.
                    if (key.length()==this.keySize && key.charAt(this.keySize -1)!=' ')
                        key = key.substring(1, this.keySize ) + ' ';
                    continue;
                }

                if (key.length()<this.keySize )
                    // Si la clave es menor de K, entonces le concatenamos el nuevo carácter leído.
                    key = key + (char) car;
                else
                    // Si la clave es igua a K, entonces eliminaos su primier carácter y le concatenamos el nuevo carácter leído (implementamos una slidding window sobre el fichero a indexar).
                    key = key.substring(1, this.keySize ) + (char) car;

                if (key.length()==this.keySize ){
                    // Si tenemos una clave completa, la añadimos al Hash, junto a su desplazamiento dentro del fichero.
                    AddKey(key, pos-this.keySize +1);
                }
            }

            f.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void AddKey(String key, long offset) {

        localHash.put(key,offset);
    }

    public HashMultimap<String, Long> getLocalHash() {
        return localHash;
    }
}
