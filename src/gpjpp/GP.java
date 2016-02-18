// gpjpp (genetic programming package for Java)
// Copyright (c) 1997, Kim Kokkonen
//
// This program is free software; you can redistribute it and/or 
// modify it under the terms of version 2 of the GNU General Public 
// License as published by the Free Software Foundation.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// Send comments, suggestions, problems to kimk@turbopower.com

package gpjpp;

import java.io.*;

/**
 * Stores the structure and status of a particular genetic program.
 * GP is a container that contains an element for the result-producing
 * branch of the program as well as for each ADF, if any. Each is a 
 * <a href="gpjpp.GPGene.html#_top_">GPGene</a> object, which forms 
 * the root of a variably sized tree whose elements are nodes from 
 * the node set for that branch.<p>
 *
 * Besides the program itself, GP also stores the complexity and
 * maximum depth of the tree, its standard fitness (as computed by a 
 * user subclass), its adjusted fitness, and its heritage. The heritage
 * consists of the population indices of the two parents if the 
 * GP was created by crossover or the population index of one parent
 * if the GP was created by straight reproduction. It also includes
 * the crossover points and the last mutation locus when applicable.<p>
 *
 * The user must always create a subclass of GP for use in solving
 * a particular problem. This subclass must override the 
 * <a href="gpjpp.GP.html#evaluate">evaluate()</a> method, which 
 * returns a double that indicates the standard fitness of the GP. 
 * (The standard fitness must always be non-negative, and
 * the genetic algorithm attempts to find individuals with minimal
 * standard fitness.) The other fields of the GP are calculated
 * by gpjpp.<p>
 *
 * Usually the user must also override the 
 * <a href="gpjpp.GP.html#createGene">createGene()</a> method to
 * dynamically create genes of a problem-specific type. See the 
 * supplied example programs for more details.
 *
 * @version 1.0
 */
public class GP extends GPContainer {

    /**
     * The standardized fitness of the GP. Must be non-negative and
     * 0.0 is considered optimal. This field is initialized with the
     * value returned by the <a href="gpjpp.GP.html#evaluate">evaluate()</a>
     * method, which must be overridden by the user.
     */
    protected double stdFitness;

    /**
     * The adjusted fitness of the GP. This value is initialized by
     * the <a href="gpjpp.GP.html#calcAdjustedFitness">
     * calcAdjustedFitness()</a> method. Adjusted fitness is always
     * in the range 0.0 to 1.0, with 1.0 being optimal.
     */ 
    protected double adjFitness;

    /**
     * The complexity, or length, of the GP. This is a count of the
     * total number of nodes in all branches of the genetic program.
     */
    protected int gpLength;

    /**
     * The maximum depth of any branch of the GP.
     */
    protected int gpDepth;

    /**
     * The population index of this GP's "dad" when it was generated
     * as a result of crossover or reproduction. This value is shown 
     * in the detail report file when enabled. Has the value -1 if 
     * the GP was generated by creation.
     *
     * @see gpjpp.GP#cross
     */
    public int dadIndex;

    /**
     * The s-expression index of this GP's "dad" when it was generated
     * as a result of crossover. Otherwise has a value of -1. The
     * index is a depth-first count of nodes to reach the crossover 
     * locus.
     */
    public int dadCross;

    /**
     * The population index of this GP's "mum" when it was generated
     * as a result of crossover. This value is shown 
     * in the detail report file when enabled. Has the value -1 if 
     * the GP was generated by creation or reproduction.
     */
    public int mumIndex;

    /**
     * The s-expression index of this GP's "mum" when it was generated
     * as a result of crossover. Otherwise has a value of -1.  The
     * index is a depth-first count of nodes to reach the crossover 
     * locus.
     */
    public int mumCross;

    /**
     * The branch number of this GP's mum and dad when it was generated
     * as a result of crossover. Otherwise has the value -1. Branch
     * number 0 is the result-producing branch, number 1 is the first
     * ADF, and so on.
     */
    public int crossTree;

    /**
     * The branch number used in 
     * <a href="gpjpp.GP.html#swapMutation">swap mutation</a> when this 
     * GP was generated. Has the value -1 if swap mutation was not 
     * involved.
     */
    public int swapTree;

    /**
     * The s-expression index used to apply swap mutation. Has the
     * value -1 if swap mutation was not involved. The
     * index is a depth-first count of nodes to reach the mutation
     * locus.
     */
    public int swapPos;

    /**
     * The branch number used in 
     * <a href="gpjpp.GP.html#shrinkMutation">shrink mutation</a> when 
     * this GP was generated. Has the value -1 if shrink mutation was 
     * not involved.
     */
    public int shrinkTree;

    /**
     * The s-expression index used to apply shrink mutation. Has the
     * value -1 if shrink mutation was not involved. The
     * index is a depth-first count of nodes to reach the mutation
     * locus.
     */
    public int shrinkPos;

    /**
     * Sets all of the heritage fields (dadIndex, dadCross, mumIndex,
     * MumCross, crossTree, swapTree, swapPos, shrinkTree, shrinkPos)
     * to -1 when a new GP is created or cloned. Used internally.
     */
    protected void clearHeritage() {
        dadIndex = -1;
        dadCross = -1;
        mumIndex = -1;
        mumCross = -1;
        crossTree = -1;
        swapTree = -1;
        swapPos = -1;
        shrinkTree = -1;
        shrinkPos = -1;
    }

    /**
     * Public null constructor used during stream loading only.
     * Note that heritage fields are not stored on streams, so 
     * this information is lost after a checkpoint.
     */
    public GP() { clearHeritage(); }

    /**
     * Constructor used when GPs are first created.
     * This constructor creates a container capable of holding the 
     * predefined branches of the GP, but does not fill in any nodes.
     *
     * @param trees  the number of branches in the GP.
     */
    public GP(int trees) {
        super(trees);
        clearHeritage();
    }

    /**
     * A constructor that is called to clone a GP. Used
     * whenever a GP is selected for reproduction or crossover.
     * The heritage of the GP is reset by this constructor because
     * it will always be updated by the population manager.
     */
    public GP(GP gpo) {
        super(gpo);
        stdFitness = gpo.stdFitness;
        adjFitness = gpo.adjFitness;
        gpLength = gpo.gpLength;
        gpDepth = gpo.gpDepth;

        //heritage is reset even during copying and cloning
        clearHeritage();
    }

    /**
     * Implements the Cloneable interface.
     * This (or its user subclass) is called during reproduction.
     *
     * @return the cloned object.
     */
    protected synchronized Object clone() { return new GP(this); }

    /**
     * Returns a code identifying the class in a stream file.
     *
     * @return the ID code GPID.
     */
    public byte isA() { return GPID; }

    /**
     * Creates a root gene while a new branch is being built. The
     * user must generally override this in a subclass to create
     * genes of user type. See the example programs.
     *
     * @param gpo  a node type that is an element of the current
     *             branch's node set. Always a function node type.
     * @return the newly created gene.
     */
    public GPGene createGene(GPNode gpo) { return new GPGene(gpo); }

    /**
     * A debugging/testing method used to ensure that no null node or 
     * gene references are found in this GP.
     *
     * @exception java.lang.RuntimeException
     *              if a null branch, gene, or node reference is found.
     */
    public void testNull() {
        for (int i = 0; i < containerSize(); i++) {
            GPGene current = (GPGene)get(i);
            if (current == null)
                throw new RuntimeException("Null tree found in GP");
            //test children of the gene
            current.testNull();
        }
    }

    /**
     * The user must override this method to evaluate and return
     * the <a href="gpjpp.GP.html#stdFitness">standardized fitness</a>
     * of the GP. The subclass method should <em>not</em> call 
     * super.evaluate().
     *
     * @exception java.lang.RuntimeException
     *              if it has not been overridden.
     */
    public double evaluate(GPVariables cfg) {
        throw new RuntimeException("Must override to evaluate each GP");
    }

    /**
     * Calculates the <a href="gpjpp.GP.html#adjFitness">
     * adjusted fitness</a> after evaluate() has computed
     * the standardized fitness. Adjusted fitness is 1/(1+stdFitness).
     * Adjusted fitness is always in the range 0.0 to 1.0 and is
     * optimal at 1.0. Adjusted fitness is used to select the best
     * individuals in 
     * <a href="gpjpp.GPPopulation.html#probabilisticSelection">
     * probabilistic</a> and 
     * <a href="gpjpp.GPPopulation.html#greedySelection">greedy</a>
     * selection. Standardized fitness is used to select the worst 
     * individuals.
     */
    protected void calcAdjustedFitness() {
        adjFitness = 1.0/(1.0+stdFitness);
    }

    /**
     * Returns the already-calculated standard fitness.
     *
     * @see gpjpp.GP#stdFitness
     * @see gpjpp.GP#evaluate
     */
    public double getFitness() { return stdFitness; }

    /**
     * Returns the already-calculated adjusted fitness.
     *
     * @see gpjpp.GP#adjFitness
     * @see gpjpp.GP#calcAdjustedFitness
     */
    public double getAdjFitness() { return adjFitness; }

    /**
     * Returns true if this GP is better than another specified
     * non-null GP. Compares standardized fitness and then uses
     * complexity as a tiebreaker. Used internally by the 
     * <a href="gpjpp.GPPopulation.html#tournamentSelection">
     * tournament</a> selection method.
     */
    protected boolean betterThan(GP gp) {
        if (stdFitness < gp.stdFitness)
            return true;
        if (stdFitness > gp.stdFitness)
            return false;
        return (length() < gp.length());
    }

    /**
     * Returns the complexity (length, or number of nodes) of the
     * GP.
     */
    public int length() { return gpLength; }

    /**
     * Returns the maximum depth of the GP.
     */
    public int depth() { return gpDepth; }

    /**
     * Calculates the complexity of the GP by calling 
     * <a href="gpjpp.GPGene.html#length">GPGene.length()</a>
     * method for each of its branches. Used internally.
     *
     * @see gpjpp.GP#length
     */
    protected void calcLength() {
        gpLength = 0;
        for (int i = 0; i < containerSize(); i++)
            gpLength += ((GPGene)get(i)).length();
    }

    /**
     * Calculates the depth of the GP by calling 
     * <a href="gpjpp.GPGene.html#depth">GPGene.depth()</a>
     * method for each of its branches. Used internally.
     *
     * @see gpjpp.GP#depth
     */
    protected void calcDepth() {
        gpDepth = 0;
        for (int i = 0; i < containerSize(); i++) {
            int imaxdepth = ((GPGene)get(i)).depth();
            if (imaxdepth > gpDepth)
                gpDepth = imaxdepth;
        }
    }

    /**
     * Returns an integer hashcode for this GP. This is used
     * internally when calculating the diversity of a population.
     * The hash code is composed from the GP's complexity, depth,
     * and the root node type of up to 10 of its branches.
     */
    public int hashCode() {
        //hash uses 8 bits of length and 4 bits of depth
        int hash = gpLength ^ (gpDepth << 8);
        int shift = 12;
        //and 2 bits for the root function of each GP branch
        for (int i = 0; (i < containerSize()) && (shift < 32); i++) {
            hash ^= ((GPGene)get(i)).geneNode().value() << shift;
            shift += 2;
        }
        return hash;
    }

    /**
     * Determines whether this GP equals another object. It returns
     * true if obj is not null, is an instance of a GP (or a
     * descendant), and has the same structure and node values as 
     * this GP. This function is called when testing the 
     * diversity of the population. equals() is called only after the
     * hashCode() function determines that two GPs are at least
     * similar.
     *
     * @param obj any Java object reference, including null.
     * @return true if this and obj are equivalent.
     */
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof GP))
            return false;
        GP gp = (GP)obj;

        if (containerSize() != gp.containerSize())
            throw new RuntimeException("Number of ADFs differs");

        //loop through all subtrees and compare them
        for (int i = 0; i < containerSize(); i++) {
            GPGene g1 = (GPGene)get(i);
            GPGene g2 = (GPGene)gp.get(i);
            if (g1 != null) {
                if (!g1.equals(g2))
                    return false;
            } else if (g2 != null)
                return false;
        }
        return true;
    }

    /**
     * Creates the branches of this GP and then creates the genes
     * of each branch according to the limits and methods specified. 
     * The GP for which this method is called should have been created
     * by calling the <a href="gpjpp.GPPopulation.html#createGP">
     * createGP()</a> method of a subclass of GPPopulation.
     * createGP() allocates a branch container of appropriate size 
     * but doesn't fill in the children.
     *
     * @param creationType  the method used to create the tree, either
     *      <a href="gpjpp.GPVariables.html#GPGROW">GPGROW</a> 
     *      (use function nodes to fill the tree to allowable depth) 
     *      or <a href="gpjpp.GPVariables.html#GPVARIABLE">GPVARIABLE</a>
     *      (choose function and terminal nodes with 50:50 probability).
     * @param allowableDepth  the maximum allowable depth of each tree.
     *      The allowable depth must always be at least 2.
     * @param allowableLength  the maximum allowable number of nodes
     *      in the GP. Since create() cannot predict how many nodes 
     *      will be added recursively it simply stops adding nodes 
     *      if it exceeds allowableLength. The 
     *      <a href="gpjpp.GPPopulation.html#create">create()</a> routine 
     *      in GPPopulation rejects the returned GP if the total 
     *      complexity exceeds allowableLength.
     * @param adfNs  the node set used to select functions and terminals
     *      for the GP.
     *
     * @exception java.lang.RuntimeException
     *              if creationType is neither GPGROW nor GPVARIABLE, or
     *              if allowableDepth is less than 2.
     */
    public synchronized void create(int creationType, 
        int allowableDepth, int allowableLength, GPAdfNodeSet adfNs) {

        if ((creationType != GPVariables.GPGROW) &&
            (creationType != GPVariables.GPVARIABLE))
            throw new RuntimeException("Argument creationType must be GPGROW or GPVARIABLE");
        if (allowableDepth < 2)
            throw new RuntimeException("allowableDepth must be at least 2");

        //decrease allowableDepth because first node is always a function
        allowableDepth--;

        //keep track of GP's complexity
        int lengthSoFar = 0;

        //loop through each adf
        for (int i = 0; i < containerSize(); i++) {
            GPNodeSet ns = (GPNodeSet)adfNs.get(i);

            //choose random function from node set and create 
            //  a compatible gene to be the root of the tree
            GPGene g = createGene(ns.chooseFunction());
            put(i, g);

            //create tree structure under the root
            lengthSoFar += g.create(creationType, allowableDepth, 
                allowableLength-lengthSoFar, ns);

            //save length and return if already too complex
            gpLength = lengthSoFar;
            if (gpLength > allowableLength)
                return;
        }

        //calculate the maximum depth
        calcDepth();
    }

    //used only in shrinkMutation and swapMutation; avoid reallocating
    static private GPGeneReference parentRef = new GPGeneReference();

    /**
     * Mutates this GP by finding a random function gene in a random
     * branch and then replacing that gene by one of its immediate
     * children. This has the effect of shrinking the tree one level.
     */
    public synchronized void shrinkMutation() {

        //select random tree
        int randTree = GPRandom.nextInt(containerSize());

        //get root gene
        GPGene rootGene = (GPGene)get(randTree);

        //initialize parent reference for searching and updating tree
        parentRef.assignContainer(this, randTree);

        //select a function gene on that branch
        if (rootGene.chooseFunctionNode(parentRef)) {
            //a function node is available
            GPGene g = parentRef.getGene();

            shrinkTree = randTree;
            shrinkPos = parentRef.count;

            //display results before mutation
            //System.out.println("before shrink mutation, shrinkPos "+shrinkPos);
            //rootGene.printOn(System.out);
            //System.out.println();

            //select random immediate child of chosen function gene
            int subTree = GPRandom.nextInt(g.containerSize());
            GPGene child = (GPGene)g.get(subTree);

            //no point in creating a tree of depth 1
            if ((g == rootGene) && child.isTerminal())
                return;

            //ensure chopped-off tree doesn't continue to refer to child
            //shouldn't be necessary, but in case of faulty gc...
            g.put(subTree, null);

            //put the child in the position of the former parent
            parentRef.putGene(child);

            //recalculate length and depth
            calcLength();
            calcDepth();

            //System.out.println("after shrink mutation, subTree "+subTree);
            //((GPGene)get(randTree)).printOn(System.out);
            //System.out.println();
            //System.in.read();
        }
    }

    /**
     * Mutates this GP by finding a random function gene in a 
     * random branch and changing the node type of that gene.
     * The node type is changed randomly to another type that has 
     * the same number of arguments. If such a node cannot be 
     * found in 5 tries, nothing is done.
     */
    //replace a randomly chosen function node's operator
    public synchronized void swapMutation(GPAdfNodeSet adfNs) {

        //select random tree and get node set and root node
        int randTree = GPRandom.nextInt(containerSize());
        GPNodeSet ns = (GPNodeSet)adfNs.get(randTree);
        GPGene rootGene = (GPGene)get(randTree);

        //initialize parent reference for searching and updating tree
        parentRef.assignContainer(this, randTree);

        //select a gene on that branch, hopefully a function
        rootGene.chooseFunctionOrTerminalNode(parentRef);
        GPGene g = parentRef.getGene();

        //get number of args to this node, if any
        int args = g.containerSize();
        int val = g.node.value();

        //try 5 times to find a node with different id but same args
        for (int i = 0; i < 5; i++) {
            //choose random node from node set with matching args
            GPNode node = ns.chooseNodeWithArgs(args);
            if (node.value() != val) {
                //replace old function with new one
                g.node = node;
                swapTree = randTree;
                swapPos = parentRef.count;
                break;
            }
        }
    }

    /**
     * Uses the configuration probabilities 
     * <a href="gpjpp.GPVariables.html#SwapMutationProbability">
     * SwapMutationProbability</a> and
     * <a href="gpjpp.GPVariables.html#ShrinkMutationProbability">
     * ShrinkMutationProbability</a> to determine whether
     * to apply either or both forms of mutation to this GP. Swap
     * mutation is considered first, then shrink mutation.
     *
     * @see gpjpp.GP#swapMutation
     * @see gpjpp.GP#shrinkMutation
     */
    public void mutate(GPVariables cfg, GPAdfNodeSet adfNs) {
        if (GPRandom.flip(cfg.SwapMutationProbability))
            swapMutation(adfNs);
        if (GPRandom.flip(cfg.ShrinkMutationProbability))
            shrinkMutation();
    }

    //used only in cross; avoid repeated allocation
    static private GPGeneReference dadRef = new GPGeneReference();
    static private GPGeneReference mumRef = new GPGeneReference();
    static private GPGeneReference dadCut = new GPGeneReference();
    static private GPGeneReference mumCut = new GPGeneReference();

    /**
     * Performs crossover on two GPs. Then a random gene, preferably a 
     * function gene, is chosen from the same random branch of each GP 
     * and the two subtrees are swapped between the two GPs.
     *
     * @param parents  a container holding the two parent GPs. Upon
     *                 exit, the same container holds the two GPs
     *                 after crossover.
     * @param maxDepthForCrossover  the maximum depth allowed for
     *                 either GP after crossover. If the result of
     *                 a crossover exceeds this depth, the subtrees
     *                 are swapped back to their original state and
     *                 another random crossover on the same branch
     *                 is attempted, continuing until an acceptable
     *                 result is achieved.
     * @param maxComplexity  the maximum complexity (number of nodes)
     *                 allowed for either GP after crossover. If the
     *                 result of a crossover exceeds this limit, the
     *                 subtrees are swapped back and another crossover
     *                 is attempted.
     * @return         the parents container, now containing the
     *                 crossed GPs.
     *
     * @exception java.lang.RuntimeException
     *                 if parents does not contain exactly 2 GPs, or
     *                 if both parents don't have the same number of
     *                 branches, or if the number of branches is zero.
     */
    // Cross the objects contained in the given container.
    public synchronized GPContainer cross(
        GPContainer parents, int maxDepthForCrossover, int maxComplexity) {

        //only two sexes allowed
        if (parents.containerSize() != 2)
            throw new RuntimeException("Only two parents allowed for crossover");

        //get mum and dad from container
        GP dad = (GP)parents.get(0);
        GP mum = (GP)parents.get(1);

        //GP's must have the same number of trees
        if (dad.containerSize() != mum.containerSize())
            throw new RuntimeException("Mum and Dad must have same number of trees");
        if (dad.containerSize() == 0)
            throw new RuntimeException("Parents contain no trees");

        //pick random adf branch to cut from
        int randTree = GPRandom.nextInt(dad.containerSize());
        dad.crossTree = randTree;
        mum.crossTree = randTree;

        //save references to Dad and Mum
        dadRef.assignContainer(dad, randTree);
        mumRef.assignContainer(mum, randTree);

        //compute partial length without randTree
        int dadPartialLength = dad.gpLength-dadRef.getGene().length();
        int mumPartialLength = mum.gpLength-mumRef.getGene().length();
        int dadLength;
        int mumLength;

        //loop until crossover results have acceptable depth and length
        do {
            int dadDepth;
            int mumDepth;

            //assign starting cut references
            dadCut.assignRef(dadRef);
            mumCut.assignRef(mumRef);

            //determine cut points within mum and dad
            dadRef.getGene().chooseFunctionOrTerminalNode(dadCut);
            mumRef.getGene().chooseFunctionOrTerminalNode(mumCut);

            //store data for crossover tracking
            dad.dadCross = dadCut.count;
            dad.mumCross = mumCut.count;
            mum.dadCross = mumCut.count;
            mum.mumCross = dadCut.count;

            //display results before crossover
            //dadDepth = dadRef.getGene().depth();
            //System.out.println("dad before (depth "+dadDepth+")");
            //System.out.println("dad index "+dad.dadIndex+"; mum index "+dad.mumIndex);
            //System.out.println("cross tree "+dad.crossTree+"; dad cross "+dad.dadCross+"; mum cross "+dad.mumCross);
            //dadRef.getGene().printOn(System.out);
            //System.out.println();
            //mumDepth = mumRef.getGene().depth();
            //System.out.println("mum before (depth "+mumDepth+")");
            //System.out.println("dad index "+mum.dadIndex+"; mum index "+mum.mumIndex);
            //System.out.println("cross tree "+mum.crossTree+"; dad cross "+mum.dadCross+"; mum cross "+mum.mumCross);
            //mumRef.getGene().printOn(System.out);
            //System.out.println();

            //swap the whole subtrees
            GPGene tmpD = dadCut.getGene();
            dadCut.putGene(mumCut.getGene());
            mumCut.putGene(tmpD);

            //compute new tree depth
            dadDepth = dadRef.getGene().depth();
            mumDepth = mumRef.getGene().depth();

            //update overall GP complexity
            dadLength = dadPartialLength+dadRef.getGene().length();
            mumLength = mumPartialLength+mumRef.getGene().length();

            //display results after crossover
            //System.out.println("dad after (depth "+dadDepth+")");
            //dadRef.getGene().printOn(System.out);
            //System.out.println();
            //System.out.println("mum after (depth "+mumDepth+")");
            //mumRef.getGene().printOn(System.out);
            //System.out.println();

            if ((dadDepth > maxDepthForCrossover) ||
                (mumDepth > maxDepthForCrossover) ||
                (dadLength > maxComplexity) ||
                (mumLength > maxComplexity)) {
                //undo crossover and try again
                tmpD = dadCut.getGene();
                dadCut.putGene(mumCut.getGene());
                mumCut.putGene(tmpD);
            } else
                //crossover acceptable
                break;
        } while (true);

        //recompute or save changed values
        dad.calcDepth();
        mum.calcDepth();
        dad.gpLength = dadLength;
        mum.gpLength = mumLength;

        //return the same container
        return parents;
    }

    /**
     * Loads a GP from the specified stream. Reads the 
     * standardized and adjusted fitness from the stream, 
     * then loads the container of gene branches. Afterwards,
     * the length and depth of the GP is recalculated.
     * The heritage of the GP is not stored and therefore is
     * lost after a save/load cycle.
     *
     * @exception java.lang.ClassNotFoundException
     *              if the class indicated by the stream's ID code
     *              is not registered with GPObject.
     * @exception java.lang.InstantiationException
     *              if an error occurs while calling new or the null
     *              constructor of the specified class.
     * @exception java.lang.IllegalAccessException
     *              if the specified class or its null constructor is
     *              not public.
     * @exception java.io.IOException
     *              if an error occurs while reading the stream.
     */
    protected synchronized void load(DataInputStream is)
        throws ClassNotFoundException, IOException,
            InstantiationException, IllegalAccessException {

        stdFitness = is.readDouble();
        adjFitness = is.readDouble();

        //load container
        super.load(is);

        //recalculate length and depth
        calcLength();
        calcDepth();
        //heritage is not stored to save space
    }

    /**
     * Saves a GP to the specified stream. Writes the 
     * standardized and adjusted fitness to the stream, 
     * then saves the container of gene branches.
     */
    protected void save(DataOutputStream os) throws IOException {

        os.writeDouble(stdFitness);
        os.writeDouble(adjFitness);
        //gpLength and gpDepth are recalculated after loading
        //heritage is not stored to save space

        //save container
        super.save(os);
    }

    /**
     * Writes a GP in text format to a PrintStream.
     * Each branch is preceded by "RPB" (for the result-producing
     * branch) or "ADFn" (for the ADFs) and continues with the
     * branch's s-expression on one line.
     *
     * @see gpjpp.GPGene#printOn
     */
    public void printOn(PrintStream os, GPVariables cfg) {

        for (int i = 0; i < containerSize(); i++) {
            if (i == 0)
                os.print("RPB:  ");
            else
                os.print("ADF"+(i-1)+": ");
            ((GPGene)get(i)).printOn(os, cfg);
            os.println();
            os.println();
        }
    }

    /**
     * Writes a GP in text tree format to a PrintStream.
     * If there are no ADFs, the main branch's tree is printed
     * without any title. Otherwise, each branch is preceded 
     * by "RPB" (for the result-producing branch) or "ADFn" 
     * (for the ADFs), which is followed by the tree in
     * pseudo-graphic format.
     *
     * @see gpjpp.GPGene#printTree
     */
    public void printTree(PrintStream os, GPVariables cfg) {

        for (int i = 0; i < containerSize(); i++) {
            if (containerSize() != 1)
                if (i == 0)
                    os.println("RPB: ");
                else
                    os.println("ADF"+(i-1)+": ");

            ((GPGene)get(i)).printTree(os, cfg);
            os.println();
        }
    }

    /**
     * Writes a GP in graphic gif file format. This method simply
     * computes a title for the gif file and then calls 
     * <a href="gpjpp.GPGene.html#drawOn">GPGene.drawOn</a>. 
     * If there are no ADFs, the title is blank; otherwise it is "RPB" 
     * for the result-producing branch and "ADFn" for the ADF branches.
     */
    public void drawOn(GPDrawing ods, String fnameBase, 
        GPVariables cfg) throws IOException {

        //scan main branch and each ADF
        for (int i = 0; i < containerSize(); i++) {
            String title;
            if (containerSize() == 1)
                title = "";
            else if (i == 0)
                title = "RPB";
            else
                title = "ADF"+(i-1);

            ((GPGene)get(i)).drawOn(ods, fnameBase+title+".gif", 
                title, cfg);
        }
    }
}
