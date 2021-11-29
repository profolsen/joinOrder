import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A quick and dirty demonstration of the following algorithm
 * Chapter 13 (Query Optimization), Figure 13.7
 * From: Database System Concepts, 6th edition by
 * Silberschatz, Korth, Sudarshan
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Three tables: ");
        Table t1 = new Table(10, "A", "B", "E");
        Table t2 = new Table(20, "B", "C", "D");
        Table t3 = new Table(15, "A", "B", "D");
        System.out.println("\t" + t1 + "\n\t" + t2 + "\n\t" + t3);
        System.out.println("Join options: ");
        Join j1 = new Join(t3, new Join(t1, t2));
        System.out.println("\t" + j1 + " " + j1.tuples());
        Join j4 = new Join(t1, new Join(t2, t3));
        System.out.println("\t" + j4 + " " + j4.tuples());
        System.out.println("full cost of best option: ");
        Join j2 = new Join(t1, t3);
        System.out.println("\t" + j2 + " " + j2.tuples());
        j2 = new Join(new Join(t1, t3), t2);
        System.out.println("\t" + j2 + " " + j2.tuples());

        System.out.println("Using algorithm: ");
        HashSet<Table> h1 = new HashSet<Table>();
        h1.add(t1);
        h1.add(t2);
        h1.add(t3);
        resetSolutions();
        Join j3 = optimize(h1);
        System.out.println("\t" + j3 + ": " + j3.tuples);
    }

    static HashMap<HashSet<Table>, Join> solutions;

    static void resetSolutions() {
        solutions = new HashMap<HashSet<Table>, Join>();
    }

    static Join optimize(HashSet<Table> tables) {
        if(solutions.containsKey(tables)) return solutions.get(tables);
        if(tables.size() == 1) return tables.iterator().next(); //base case
        if(tables.size() == 2) { //base case.
            Table left, right;
            Iterator<Table> it = tables.iterator();
            left = it.next();
            right = it.next();
            return new Join(left, right);
        }
        //recursive case:
        Join best = null;
        for(HashSet<Table> s1 : subsets(tables)) {
            HashSet<Table> others = without(tables, s1);
            if(others.size() == 0) continue;  //skip empty subsets.
            Join j1 = optimize(s1);
            Join j2 = optimize(others);
            Join current = new Join(j1, j2);
            //System.out.println("current = " + current);
            if(best == null || current.tuples < best.tuples) best = current;
        }
        solutions.put(tables, best);
        return best;
    }

    private static HashSet<HashSet<Table>> subsets(HashSet<Table> in) {
        HashSet<HashSet<Table>> subsets = new HashSet<HashSet<Table>>();
        boolean[] include = new boolean[in.size()];
        Table[] tables = new Table[in.size()];
        int i = 0;
        for(Table t : in) tables[i++] = t;
        include[0] = true;
        while(notAllFalse(include)) {
            HashSet<Table> s = new HashSet<Table>();
            for(i = 0; i < tables.length; i++) {
                if(include[i]) s.add(tables[i]);
            }
            subsets.add(s);
            increment(include);
        }
        return subsets;
    }

    private static boolean notAllFalse(boolean[] array) {
        for(boolean b : array) if(b) return true;
        return false;
    }

    private static void increment(boolean[] array) {
        int i = 0;
        while(i < array.length && array[i]) {
            array[i] = false;
            i++;
        }
        if(i < array.length) array[i] = true;
    }

    //returns X - Y (set minus)
    private static HashSet<Table> without(HashSet<Table> x, HashSet<Table> y) {
        HashSet<Table> answer = new HashSet<Table>();
        answer.addAll(x);
        answer.removeAll(y);
        //System.out.println(answer);
        return answer;
    }

}

//weirdness: this is easier to do if Table extends Join but that
//goes against how I think about the problem, i.e., that Join should extend Table.
//cost of accessing a table is the number of tuples in the table.
//contrived, but gives something for the algorithm to work with.
class Table extends Join {

    static int idSource = 1;
    int id = idSource++;

    public Table(int tuples, String... attributes) {
        super(null, null);
        this.tuples = tuples;
        for(String s : attributes) {
            this.attributes.add(s);
        }
    }

    @Override
    public String toString() {
        return "T" + id + attributes + ": " + tuples;
    }

}

//cost model of a two table join on R1(A1, A2, ..., An) and R2(B1, B2, ..., Bm)
//with |R1| = # of tuples in R1
//and |R2| = # of tuples in R2
//and CC(R1, R2) = # of columns (attributes) in common between R1 and R2:
//cost(R1 join R2) = |R1| * |R2| * 1/(2^CC(R1, R2).
//admittedly, super contrived, but gives something to work with so the algorithm
//can be tested, etc.
class Join {
    int tuples = 0;
    Integer hashCode = null;
    Join left = null, right = null;
    HashSet<String> attributes = new HashSet<String>();

    public Join(Join left, Join right) {
        this.left = left;
        this.right = right;
        if(left != null && right != null) {
            attributes.addAll(left.attributes);
            attributes.addAll(right.attributes);
            double factor = 1;
            int duplicatedColumns = left.attributes.size() + right.attributes.size() - attributes.size();
            while(duplicatedColumns-- > 0) factor = factor * 0.5;
            tuples = (int)(factor * left.tuples() * right.tuples());
        }
    }

    @Override
    public int hashCode() {
        if(hashCode != null) return hashCode;
        hashCode = 0;
        for(String s : attributes) {
            hashCode ^= s.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Table) {
            HashSet<String> t = new HashSet<String>();
            t.addAll(attributes);
            t.addAll(((Table) o).attributes);
            boolean noneExtra = t.size() == attributes.size();
            t.removeAll(((Table) o).attributes);
            boolean noneMissing = t.size() == 0;
            return noneExtra && noneMissing && tuples == ((Table) o).tuples;
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + left + " x " + right + ")";
    }

    public int tuples() {  return tuples;  }
}
