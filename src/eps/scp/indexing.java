
/* ---------------------------------------------------------------
Práctica 2.
Código fuente : Indexing.java
Grau Informàtica Sistemas Concurrents y Parallels.
18068091G José Miguel Avellana Lopez.
X8592934L Yassine El Kihal.
--------------------------------------------------------------- */
public class indexing {

    public static void main(String[] args)
    {
        InvertedIndex hash;

        if (args.length <2 || args.length>4)
            System.err.println("Erro in Parameters. Usage: Indexing <TextFile> [<Key_Size>] [<Index_Directory>]");
        if (args.length < 2)
            hash = new InvertedIndex(args[0]);
        else if (args.length == 2)
            hash = new InvertedIndex(args[0], Integer.parseInt(args[1]));  //inputFile, keySize
        else
            hash = new InvertedIndex(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2])); //inputFile, numHilos, KeySize

        hash.BuildIndex();

        System.out.println("--------------MAIN--print index");

        if (args.length > 3)
            hash.SaveIndex(args[3]);
        else
            hash.PrintIndex();
    }

}

