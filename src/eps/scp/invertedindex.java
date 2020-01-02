package eps.scp;

import com.google.common.collect.HashMultimap;
import org.apache.commons.lang3.StringUtils;
import java.lang.Thread;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/* ---------------------------------------------------------------
Práctica 2.
Código fuente : InvertedIndex.java
Grau Informàtica Sistemas Concurrents y Parallels.
18068091G José Miguel Avellana Lopez.
X8592934L Yassine El Kihal.
--------------------------------------------------------------- */

public class InvertedIndex {
    // Constantes
    private final int DNumHilos = 1;
    private final int DKeySize = 10;            // Tamaño de la clave/ k-word por defecto.
    private final int DIndexMaxNumberOfFiles = 1000;   // Número máximio de ficheros para salvar el índice invertido.
    private final String DIndexFilePrefix = "/IndexFile";   // Prefijo de los ficheros de Índice Invertido.
    private final float DMinimunMatchingPercentage = 0.80f;  // Porcentaje mínimo de matching entre el texto original y la consulta (80%)
    private final int DPaddingMatchText = 20;   // Al mostrar el texto original que se corresponde con la consulta se incrementa en 20 carácteres
    //private final int DChunkSize = 100;

    // Members
    private int numHilos;
    private String InputFilePath;       // Contiene la ruta del fichero a Indexar.
    private RandomAccessFile randomInputFile;  // Fichero random para acceder al texto original con mayor porcentaje de matching.
    private int KeySize;            // Número de carácteres de la clave (k-word)
    private HashMultimap<String, Long> hashGlobal = HashMultimap.create();    // hashGlobal Map con el Índice Invertido.

    // Constructores
    public InvertedIndex() {
        InputFilePath = null;
        KeySize = DKeySize;
        numHilos = DNumHilos;
    }

    public InvertedIndex(String inputFile) {
        this();
        InputFilePath = inputFile;
    }

    public InvertedIndex(int keySize) {
        this();
        KeySize = keySize;
    }

    public InvertedIndex(String inputFile, int keySize) {
        this.InputFilePath = inputFile;
        this.KeySize = keySize;
        this.numHilos = DNumHilos;
    }

    public InvertedIndex(String inputFile, int numHilos, int keySize) {
        this.InputFilePath = inputFile;
        this.KeySize = keySize;
        this.numHilos = numHilos;
    }

    public void SetFileName(String inputFile) {
        InputFilePath = inputFile;
    }


    private Boolean isNotValid(long length){
        return length/numHilos < KeySize;
    }

    /* Método para construir el indice invertido, utilizando un HashMap para almacenarlo en memoria */
    public void BuildIndex(){

        BuildIndexThread hilos[] = new BuildIndexThread[numHilos];  //Array de hilos.
        Thread threads[] = new Thread[numHilos];

        try {
            File file = new File(InputFilePath);

            if (isNotValid(file.length())){
                System.out.println("ERROR: Variables incompatibles.");
                System.exit(-1);
            }

            // El portionSize define la porcion de archivo por cada hilo.
            long portionSize;
            //Reparticion en hilos.
            int hilo=0;
            for (long i = 0; i < file.length(); i += portionSize) {
                portionSize=((file.length()-i)/(numHilos-hilo));
                hilos[hilo] = new BuildIndexThread(InputFilePath , i, i+portionSize);
                threads[hilo] = new Thread(hilos[hilo]);
                threads[hilo].start();
                hilo++;
            }

            //Une cada hashLocal cuando cada hilo termina.
            for (int i = 0; i < numHilos; i++) {
                try {
                    threads[i].join();
                    hashGlobal.putAll(hilos[i].hashLocal);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        } catch (FileNotFoundException fnfE) {
            System.err.println("Error opening Input file.");
        }
    }

    public class BuildIndexThread implements Runnable {
        RandomAccessFile is;
        long fin_portion; long start_portion;
        int car;
        String key="";
        HashMultimap<String, Long> hashLocal = HashMultimap.create();

        BuildIndexThread(String InputFilePath, long start, long fin) throws FileNotFoundException {
            this.is = new RandomAccessFile(InputFilePath, "r");
            this.fin_portion = fin;
            this.start_portion = start;
            System.out.println(start_portion);
            System.out.println(fin_portion);
        }

        public void run() {
            try {
                long offset;
                if(start_portion>0){
                    offset = start_portion-(KeySize-1);
                }else {
                    offset = start_portion;
                }
                this.is.seek(offset);
                for( ; offset < this.fin_portion && (car = is.read())!=-1; offset++) {
                    if (car == '\n' || car == '\r' || car == '\t') {
                        // Sustituimos los carácteres de \n,\r,\t en la clave por un espacio en blanco.
                        if (key.length() == KeySize && key.charAt(KeySize - 1) != ' ')
                            key = key.substring(1, KeySize) + ' ';
                        continue;
                    }
                    if (key.length() < KeySize)
                        // Si la clave es menor de K, entonces le concatenamos el nuevo carácter leído.
                        key = key + (char) car;
                    else
                        // Si la clave es igua a K, entonces eliminaos su primier carácter y le concatenamos el nuevo carácter leído (implementamos una slidding window sobre el fichero a indexar).
                        key = key.substring(1, KeySize) + (char) car;
                    if (key.length() == KeySize)
                        // Si tenemos una clave completa, la añadimos al Hash, junto a su desplazamiento dentro del fichero.
                        AddKey(key, offset - KeySize + 1, hashLocal);
                }
            }catch (IOException e){
                System.err.println("Error read Input file.");
            }
        }
    }

    // Método que añade una k-word y su desplazamiento en el HashMap.
    private void AddKey(String key, long offset, HashMultimap<String, Long> hashLocal) {
        hashLocal.put(key, offset);
        System.out.print(offset + "\t-> " + key + "\r");
    }

    // Método para imprimir por pantalla el índice invertido.
    public void PrintIndex() {
        Set<String> keySet = hashGlobal.keySet();
        Iterator keyIterator = keySet.iterator();
        while (keyIterator.hasNext()) {
            String key = (String) keyIterator.next();
            System.out.print(key + "\t");
            Collection<Long> values = hashGlobal.get(key);
            for (Long value : values) {
                System.out.print(value + ",");
            }
            System.out.println();
        }
    }

     // Método para salvar en disco el índice invertido.
    // Recibe la ruta del directorio en donde se van a guardar los ficheros del indice.

    public void SaveIndex(String outputDirectory) {
        SaveIndexThread hilos[] = new SaveIndexThread[numHilos];  //Array de hilos.
        Set<String> KeySet = hashGlobal.keySet();
	
    long portionSize;
    int hilo=0;
    // Bucle para recorrer los hilos.
        for (int i = 0; i < KeySet.size(); i += portionSize) {
            portionSize=((KeySet.size()-i)/(numHilos-hilo));
            hilos[hilo] = new SaveIndexThread(hashGlobal, outputDirectory, portionSize, i);
            hilos[hilo].start();
            hilo++;
        }
    }


    // Método para salvar una clave y sus ubicaciones en un fichero.
    public void SaveIndexKey(String key, BufferedWriter bw) {
        try {
            Collection<Long> values = hashGlobal.get(key);
            ArrayList<Long> offList = new ArrayList<Long>(values);
            // Creamos un string con todos los offsets separados por una coma.
            String joined = StringUtils.join(offList, ",");
            bw.write(key + "\t");
            bw.write(joined + "\n");
        } catch (IOException e) {
            System.err.println("Error writing Index file");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public class SaveIndexThread extends Thread {

        int numberOfFiles, remainingFiles;
        long remainingKeys = 0, keysByFile = 0;
        String key = "";
        Charset utf8 = StandardCharsets.UTF_8;
        Set<String> keySet;
        Iterator keyIterator;
        Set<String> KeySet;
        String outputDirectory;
        HashMultimap<String, Long> hashLocal = HashMultimap.create();
        long portionSize;
        int i;

        SaveIndexThread(HashMultimap<String, Long> hashLocal, String outputDirectory, long portionSize, int i) {
            KeySet = hashLocal.keySet();
            this.i = i;
            this.portionSize = portionSize;
        }

        public void run() {

            keyIterator = keySet.iterator();
            remainingKeys = keySet.size();
            remainingFiles = numberOfFiles;

            for (int j = 0; j < KeySet.size(); j++) {
                if (j < i || j >= i + portionSize) {
                    hashLocal.removeAll(hashLocal.toString().toCharArray()[j]);
                }
            }

            // Calculamos el número de ficheros a crear en función del núemro de claves que hay en el hash.
            if (keySet.size() > DIndexMaxNumberOfFiles / numHilos)
                numberOfFiles = DIndexMaxNumberOfFiles / numHilos;
            else
                numberOfFiles = keySet.size();
            // Bucle para recorrer los ficheros de indice a crear.
            for (int f = 1; f <= numberOfFiles; f++) {
                try {
                    File KeyFile = new File(outputDirectory + DIndexFilePrefix + String.format("%03d", f));
                    FileWriter fw = new FileWriter(KeyFile);
                    BufferedWriter bw = new BufferedWriter(fw);
                    // Calculamos el número de claves a guardar en este fichero.
                    keysByFile = remainingKeys / remainingFiles;
                    remainingKeys -= keysByFile;
                    // Recorremos las claves correspondientes a este fichero.
                    while (keyIterator.hasNext() && keysByFile > 0) {
                        key = (String) keyIterator.next();
                        SaveIndexKey(key, bw);  // Salvamos la clave al fichero.
                        keysByFile--;
                    }
                    bw.close(); // Cerramos el fichero.
                    remainingFiles--;
                } catch (IOException e) {
                    System.err.println("Error opening Index file " + outputDirectory + "/IndexFile" + f);
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }


    // Método para cargar en memoria (HashMap) el índice invertido desde su copia en disco.
    public void LoadIndex(String inputDirectory) {
        File folder = new File(inputDirectory);
        File[] listOfFiles = folder.listFiles();

        LoadIndexThread hilos[] = new LoadIndexThread[numHilos];  //Array de hilos.
        long portionSize;
        int hilo=0;
        for (int i = 0; i < listOfFiles.length; i += portionSize) {
            portionSize=((listOfFiles.length-i)/(numHilos-hilo));
            hilos[hilo] = new LoadIndexThread(listOfFiles);
            hilos[hilo].start();
            hilo++;
        }

        //Une cada hashLocal cuando cada hilo termina.
        for (int i = 0; i < numHilos; i++) {
            try {
                hilos[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            hashGlobal.putAll(hilos[i].hashLocal);
        }

    }

    public class LoadIndexThread extends Thread {

        File[] listOfFiles;
        HashMultimap hashLocal = HashMultimap.create();

        LoadIndexThread(File[] listOfFiles) {
        }

        public void run() {

            // Recorremos todos los ficheros del directorio de Indice y los procesamos.
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    //System.out.println("Processing file " + folder.getPath() + "/" + file.getName()+" -> ");
                    try {
                        FileReader input = new FileReader(file);
                        BufferedReader bufRead = new BufferedReader(input);
                        String keyLine = null;
                        try {
                            // Leemos fichero línea a linea (clave a clave)
                            while ((keyLine = bufRead.readLine()) != null) {
                                // Descomponemos la línea leída en su clave (k-word) y offsets
                                String[] fields = keyLine.split("\t");
                                String key = fields[0];
                                String[] offsets = fields[1].split(",");
                                // Recorremos los offsets para esta clave y los añadimos al HashMap
                                for (int i = 0; i < offsets.length; i++) {
                                    long offset = Long.parseLong(offsets[i]);
                                    hashLocal.put(key, offset);
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading Index file");
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        System.err.println("Error opening Index file");
                        e.printStackTrace();
                    }
                }
            }
        }

    }
    public void Query(String queryString) {
        String queryResult = null;
        Map<Long, Integer> offsetsFreq, sorted_offsetsFreq;

        System.out.println("Searching for query: " + queryString);

        // Split Query in keys & Obtain keys offsets
        offsetsFreq = GetQueryOffsets(queryString);

        // Sort offsets by Frequency in descending order
        sorted_offsetsFreq = SortOffsetsFreq(offsetsFreq);
        //PrintOffsetsFreq(sorted_offsetsFreq);

        // Show results (offsets>Threshold)
        try {
            // Open original input file for random access.
            randomInputFile = new RandomAccessFile(InputFilePath, "r");
        } catch (FileNotFoundException e) {
            System.err.println("Error opening input file");
            e.printStackTrace();
        }
        int maxFreq = (queryString.length() - KeySize) + 1;
        Iterator<Map.Entry<Long, Integer>> itr = sorted_offsetsFreq.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Long, Integer> entry = itr.next();
            // Calculamos el porcentaje de matching y si es superior al mínimo requerido imprimimos el resultado (texto en esta posición del fichero original)
            if (((float) entry.getValue() / (float) maxFreq) >= DMinimunMatchingPercentage)
                PrintMatching(entry.getKey(), queryString.length(), (float) entry.getValue() / (float) maxFreq);
            else
                break;
        }

        try {
            randomInputFile.close();
        } catch (IOException e) {
            System.err.println("Error opening input file");
            e.printStackTrace();
        }
    }

    // Obtenemos un Map con todos la frecuencia de aparicioón de los offssets asociados con las keys (k-words)
    // generadas a partir de la consulta
    private Map<Long, Integer> GetQueryOffsets(String query) {
        Map<Long, Integer> offsetsFreq = new HashMap<Long, Integer>();
        int queryLenght = query.length();
        // Recorremos todas las keys (k-words) de la consulta
        for (int k = 0; k <= (queryLenght - KeySize); k++) {
            String key = query.substring(k, k + KeySize);
            // Obtenemos y procesamos los offsets para esta key.
            for (Long offset : GetKeyOffsets(key)) {
                // Increase the number of occurrences of the relative offset (offset-k).
                Integer count = offsetsFreq.get(offset - k);
                if (count == null)
                    offsetsFreq.put(offset - k, 1);
                else
                    offsetsFreq.put(offset - k, count + 1);
            }
        }
        return offsetsFreq;
    }

    // Obtenes los offsets asociados con una key
    private Collection<Long> GetKeyOffsets(String key) {
        return hashGlobal.get(key);
    }


    // Ordenamos la frecuencia de aparición de los offsets de mayor a menor
    private Map<Long, Integer> SortOffsetsFreq(Map<Long, Integer> offsetsFreq) {
        List<Map.Entry<Long, Integer>> list = new LinkedList<Map.Entry<Long, Integer>>(offsetsFreq.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<Long, Integer>>() {
            public int compare(Map.Entry<Long, Integer> o1, Map.Entry<Long, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<Long, Integer> sortedMap = new LinkedHashMap<Long, Integer>();
        for (Map.Entry<Long, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    // Imprimimos la frecuencia de aparición de los offsets.
    private void PrintOffsetsFreq(Map<Long, Integer> offsetsFreq) {
        Iterator<Map.Entry<Long, Integer>> itr = offsetsFreq.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Long, Integer> entry = itr.next();
            System.out.println("Offset " + entry.getKey() + " --> " + entry.getValue());
        }
    }

    // Imprimimos el texto de un matching de la consulta.
    // A partir del offset se lee y se imprime tantos carácteres como el tamaño de la consulta + N caracteres de padding.
    private void PrintMatching(Long offset, int length, float perMatching) {
        byte[] matchText = new byte[length + DPaddingMatchText];

        try {
            // Nos posicionamos en el offset deseado.
            randomInputFile.seek(offset.intValue());
            // Leemos el texto.
            randomInputFile.read(matchText);
        } catch (IOException e) {
            System.err.println("Error reading input file");
            e.printStackTrace();
        }
        System.out.println("Matching at offset " + offset + " (" + perMatching * 100 + "%): " + new String(matchText));
    }
}

