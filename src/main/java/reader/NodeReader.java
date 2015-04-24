package reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import graph.*;
public class NodeReader {
  
  public static Graph ReadNodes(Scanner sc) {
    ArrayList<Node> nodes = new ArrayList<Node>();
    while(sc.hasNextLine()){
      String nextnode = sc.nextLine();
      if(nextnode.charAt(0)=='>'){
        nextnode = nextnode.substring(1);
        String[] n = nextnode.replace("\\s", "").split("\\|");
        if(n.length!=4) {
          //TODO some error
        }
        String content = sc.nextLine();
        //TODO some error if not int
        int id = Integer.parseInt(n[0]);
        int start = Integer.parseInt(n[2]);
        int end = Integer.parseInt(n[3]);
        ArrayList<String> sources = new ArrayList(Arrays.asList(n[1].split(",")));
        Node node = new Node(id,sources,start,end,content);
        nodes.add(node);
      }
      else {
        //TODO some error or message?
      }
    }
    return new Graph(nodes);
  }
}
