

public class indexing {

    public static void main(String[] args) {
        InvertedIndex totalHash = new InvertedIndex();

        InvertedIndex hash;

        if (args.length <2 || args.length>4) {
            System.err.println("Erro in Parameters. Usage: Indexing <TextFile> [<Key_Size>] [<Index_Directory>]");
        }
        if (args.length < 2) {
            System.out.println("<TextFile> " + args[0]);
            System.out.println("<Threads_Number> " + args[1]);

            hash = new InvertedIndex(args[0]);
        }else{
            hash = new InvertedIndex(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }

        hash.ConcBuildIndex();


        if (args.length > 2) {
            hash.SaveIndex(args[3]);
            System.out.println("Done!\r");
        }else{
            hash.PrintIndex();
        }


        //args[0]  "<TextFile> "+
        //args[1] "<Threads_Number> "+
        //args[2] "[<Key_Size>] "+
        //args[3] "[<Index_Directory>] "+

    }

}
