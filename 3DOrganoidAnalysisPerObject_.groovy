import ij.IJ
import ij.ImagePlus
import ij.gui.Roi
import ij.gui.ShapeRoi
import ij.measure.ResultsTable
import ij.plugin.ChannelSplitter
import ij.plugin.Duplicator
import ij.plugin.RGBStackMerge
import ij.plugin.filter.ThresholdToSelection
import ij.plugin.frame.RoiManager
import ij.process.FloatProcessor
import ij.process.ImageProcessor
import inra.ijpb.color.CommonColors
import loci.plugins.BF
import loci.plugins.in.ImporterOptions
import ij.plugin.frame.RoiManager
import java.awt.Color
import java.io.File;
import inra.ijpb.data.image.ColorImages;
import ij.WindowManager
import ij.Prefs
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import mcib3d.geom.Object3D
import net.imglib2.converter.ChannelARGBConverter
import org.apache.commons.compress.utils.FileNameUtils
import mcib3d.geom.Objects3DPopulation
import mcib3d.image3d.ImageInt

// INPUT UI
//
#@File(label = "Input Segmentation Files Directory", style = "directory") inputFilesOrganoid
#@File(label = " Input Raw Files Directory", style = "directory") inputFilesRaw
#@File(label = "output directory", style = "directory") outputDir
#@Integer(label = "Tomato Channel", value = 1) tomaChannel
#@Integer(label = "GFP Channel", value = 0) gfpChannel



// IDE
//
//
/*def inputFilesOrganoid = new File("/mnt/imgserver/CONFOCAL/IA/Projects/2024/2024_06_05_elapi/output/labels")
def inputFilesRaw = new File("/mnt/imgserver/CONFOCAL/IA/Projects/2024/2024_06_05_elapi/output/images_cal")
def inputFilesCal = new File("/mnt/imgserver/CONFOCAL/IA/Projects/2024/2024_06_05_elapi/output/images_noCal")
def outputDir = new File("/mnt/imgserver/CONFOCAL/IA/Projects/2024/2024_06_05_elapi/output")
def tomaChannel = 1.intValue()
def gfpChannel = 0.intValue()*/
//def headless = true;
//new ImageJ().setVisible(true);

IJ.log("-Parameters selected: ")
IJ.log("    -Input Seg Files Dir: " + inputFilesOrganoid)
IJ.log("    -Input Raw Files Dir: " + inputFilesRaw)
IJ.log("    -output Dir: " + outputDir)
IJ.log("    -Red Channel: " + tomaChannel)
IJ.log("    -Green Channel: " + gfpChannel)
IJ.log("                                                           ");
/** Get files (images) from input directory */
def listOfFiles = inputFilesRaw.listFiles();

def meanIntTomatoList = new ArrayList<Double>()
def meanIntGfpList = new ArrayList<Double>()


for (def i = 0; i < listOfFiles.length; i++) {

    if (!listOfFiles[i].getName().contains("Thumbs")) {
        /** Create image for each file in the input directory */
        def imps = new ImagePlus(inputFilesRaw.getAbsolutePath() + File.separator + listOfFiles[i].getName())
        IJ.log(imps.getTitle())
        def cal = imps.getCalibration()
        /** Split channels */
        def channels = ChannelSplitter.split(imps)
        def chTomato, chGfp, labelOrganoid = null

        /** Get astrophere labels */
        labelOrganoid = new ImagePlus(inputFilesOrganoid.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        labelOrganoid.setCalibration(cal)
        def impRaw = new ImagePlus(inputFilesRaw.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        def pixelSize = impRaw.getCalibration().pixelWidth / (2.0)
        IJ.run(labelOrganoid, "Set Scale...", String.format("distance=%f known=1 unit=micron", pixelSize));

        /** Get Tomato channel */
        chTomato = channels[tomaChannel.intValue()];

        /** Get GFP channel */
        chGfp = channels[gfpChannel.intValue()];

        // Organoid Labels to Rois
        def roisOrganoid = L2RA(labelOrganoid)

        def roisOrganoidID = new ArrayList<String>();
        for (def l = 0.intValue(); l < roisOrganoid.size(); l++)
            roisOrganoidID.add(roisOrganoid.get(l).getName().replaceAll("0001 - ID ", ""))


        // Get Organoids objects population
        def imgOrganoid = ImageInt.wrap(extractCurrentStack(labelOrganoid));
        def populationOrganoid = new Objects3DPopulation(imgOrganoid);
        // Get Tomato signal
        def signalTomato = ImageInt.wrap(extractCurrentStack(chTomato));
        // Get GFP signal
        def signalGFP = ImageInt.wrap(extractCurrentStack(chGfp));



        for (int r = 0; r < populationOrganoid.getNbObjects(); r++) {
            meanIntTomatoList.add(populationOrganoid.getObject(r).getPixMeanValue(signalTomato))
            meanIntGfpList.add(populationOrganoid.getObject(r).getPixMeanValue(signalGFP))
        }

    }
}
def meanIntTomato = meanIntTomatoList.stream()
        .mapToDouble(d -> d)
        .average()
        .orElse(0.0)
IJ.log(meanIntTomato + "------" + meanIntTomatoList.size())
def stdIntTomato = std(meanIntTomatoList, meanIntTomato)

def meanIntGfp = meanIntGfpList.stream()
        .mapToDouble(d -> d)
        .average()
        .orElse(0.0)
def stdIntGfp = std(meanIntGfpList, meanIntGfp)

for (def i = 0; i < listOfFiles.length; i++) {

    if (!listOfFiles[i].getName().contains("Thumbs")) {
        def tablePerObject = new ResultsTable();
        def tablePerWell = new ResultsTable();
        /** Create image for each file in the input directory */
        def imps = new ImagePlus(inputFilesRaw.getAbsolutePath() + File.separator + listOfFiles[i].getName())
        IJ.log(imps.getTitle())
        def cal = imps.getCalibration()
        /** Split channels */
        def channels = ChannelSplitter.split(imps)
        def chTomato, chGfp, labelOrganoid = null

        /** Get astrophere labels */
        labelOrganoid = new ImagePlus(inputFilesOrganoid.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        labelOrganoid.setCalibration(cal)
        def impRaw = new ImagePlus(inputFilesRaw.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        def pixelSize = impRaw.getCalibration().pixelWidth / 2
        IJ.run(labelOrganoid, "Set Scale...", String.format("distance=%f known=1 unit=micron", pixelSize));

        /** Get Tomato channel */
        chTomato = channels[tomaChannel.intValue()];

        /** Get GFP channel */
        chGfp = channels[gfpChannel.intValue()];

        // Organoid Labels to Rois
        def roisOrganoid = L2RA(labelOrganoid)

        def roisOrganoidID = new ArrayList<String>();
        for (def l = 0.intValue(); l < roisOrganoid.size(); l++)
            roisOrganoidID.add(roisOrganoid.get(l).getName().replaceAll("0001 - ID ", ""))


        // Get Organoids objects population
        def imgOrganoid = ImageInt.wrap(extractCurrentStack(labelOrganoid));
        def populationOrganoid = new Objects3DPopulation(imgOrganoid);
        // Get Tomato signal
        def signalTomato = ImageInt.wrap(extractCurrentStack(chTomato));
        // Get GFP signal
        def signalGFP = ImageInt.wrap(extractCurrentStack(chGfp));

        // Crete merge (labels + channels)
        def merge = RGBStackMerge.mergeChannels(new ImagePlus[]{labelOrganoid, chGfp, chTomato}, false)
        IJ.saveAs(merge, "Tiff", outputDir.getAbsolutePath() + File.separator + "merge" + File.separator + listOfFiles[i].getName())
        def volumeMicrons = new ArrayList<Double>()
        def volumePix = new ArrayList<Double>()
        def maxIntTomato = new ArrayList<Double>()
        def maxIntGFP = new ArrayList<Double>()
        def minIntTomato = new ArrayList<Double>()
        def minIntGFP = new ArrayList<Double>()
        def meanIntTomatoL = new ArrayList<Double>()
        def meanIntGFP = new ArrayList<Double>()
        def sumIntTomato = new ArrayList<Double>()
        def sumIntGFP = new ArrayList<Double>()
        def nGFP = new ArrayList<Double>()
        def nTomato = new ArrayList<Double>()
        def nFull = new ArrayList<Double>()
        def nEmpty = new ArrayList<Double>()

        def nClassGFP = 0.intValue(), nClassTomato = 0.intValue(), nLabelFull = 0.intValue(), nLabelEmpty = 0.intValue()
        def gfpPositive = new ArrayList<Double>()
        for (int r = 0; r < populationOrganoid.getNbObjects(); r++) {
            tablePerObject.incrementCounter()
            //Per Image Values
            if (populationOrganoid.getObject(r).getPixMeanValue(signalGFP) > meanIntGfp - 2 * stdIntGfp || populationOrganoid.getObject(r).getPixMeanValue(signalTomato) > meanIntTomato - 2 * stdIntTomato) {
                tablePerObject.setValue("Well Title", r, listOfFiles[i].getName())
                tablePerObject.setValue("Object ID", r, populationOrganoid.getObject(r).getValue().toString())
                tablePerObject.setValue("Volume (microns)", r, (populationOrganoid.getObject(r).getVolumeUnit() * 2).toString())
                volumeMicrons.add((populationOrganoid.getObject(r).getVolumeUnit() * 2))
                tablePerObject.setValue("Volume (pixels)", r, (populationOrganoid.getObject(r).getVolumePixels() * 2).toString())
                volumePix.add((populationOrganoid.getObject(r).getVolumePixels().doubleValue() * 2))
                tablePerObject.setValue("Max Intensity Tomato", r, populationOrganoid.getObject(r).getPixMaxValue(signalTomato).toString())
                maxIntTomato.add(populationOrganoid.getObject(r).getPixMaxValue(signalTomato))
                tablePerObject.setValue("Min Intensity Tomato", r, populationOrganoid.getObject(r).getPixMinValue(signalTomato).toString())
                minIntTomato.add(populationOrganoid.getObject(r).getPixMinValue(signalTomato))
                tablePerObject.setValue("Mean Intensity Tomato", r, populationOrganoid.getObject(r).getPixMeanValue(signalTomato).toString())
                meanIntTomatoL.add(populationOrganoid.getObject(r).getPixMeanValue(signalTomato))
                tablePerObject.setValue("Sum Intensity Tomato", r, populationOrganoid.getObject(r).getIntegratedDensity(signalTomato).toString())
                sumIntTomato.add(populationOrganoid.getObject(r).getIntegratedDensity(signalTomato))

                tablePerObject.setValue("Max Intensity GFP", r, populationOrganoid.getObject(r).getPixMaxValue(signalGFP).toString())
                maxIntGFP.add(populationOrganoid.getObject(r).getPixMaxValue(signalGFP))
                tablePerObject.setValue("Min Intensity GFP", r, populationOrganoid.getObject(r).getPixMinValue(signalGFP).toString())
                minIntGFP.add(populationOrganoid.getObject(r).getPixMinValue(signalGFP))
                tablePerObject.setValue("Mean Intensity GFP", r, populationOrganoid.getObject(r).getPixMeanValue(signalGFP).toString())
                meanIntGFP.add(populationOrganoid.getObject(r).getPixMeanValue(signalGFP))
                tablePerObject.setValue("Sum Intensity GFP", r, populationOrganoid.getObject(r).getIntegratedDensity(signalGFP).toString())
                sumIntGFP.add(populationOrganoid.getObject(r).getIntegratedDensity(signalGFP))

                if (populationOrganoid.getObject(r).getErodedObject(15,15,0).getPixMeanValue(signalGFP) > meanIntGfp) {
                    tablePerObject.setValue("Class", r, "GFP")
                    nClassGFP++
                    nGFP.add(nClassGFP.doubleValue())
                    //gfpPositive.add(populationOrganoid.getObject(r).getPixMeanValue(signalGFP))
                    if (populationOrganoid.getObject(r).getErodedObject(15,15,0).getPixMeanValue(signalGFP) > meanIntGfp+2*stdIntGfp) {
                        tablePerObject.setValue("GFP Label", r, "Full")
                        nLabelFull++
                        nFull.add(nLabelFull.doubleValue())
                    } else {
                        tablePerObject.setValue("GFP Label", r, "Empty")
                        nLabelEmpty++
                        nEmpty.add(nLabelEmpty.doubleValue())
                    }
                } else {
                    tablePerObject.setValue("Class", r, "Tomato")
                    nClassTomato++
                    nTomato.add(nClassTomato.doubleValue())
                    if (populationOrganoid.getObject(r).getErodedObject(15,15,0).getPixMeanValue(signalTomato) > meanIntTomato+2*stdIntTomato) {
                        tablePerObject.setValue("GFP Label", r, "Full")
                        nLabelFull++
                        nFull.add(nLabelFull.doubleValue())
                    } else {
                        tablePerObject.setValue("GFP Label", r, "Empty")
                        nLabelEmpty++
                        nEmpty.add(nLabelEmpty.doubleValue())
                    }
                }


            }


        }
        tablePerObject.saveAs(outputDir.getAbsolutePath() + File.separator + "csv" + File.separator + listOfFiles[i].getName() + "_perObject_table_results_erode.csv")
//        tablePerWell.incrementCounter()
//        tablePerWell.setValue("Well Title", i, listOfFiles[i].getName())
//        tablePerWell.setValue("N of Organoids", i, populationOrganoid.getNbObjects().toString())
//        tablePerWell.setValue("Volume Mean (microns) ", i, volumeMicrons.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Volume Mean (pixels) ", i, volumePix.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//
//        tablePerWell.setValue("Mean Intensity Max Tomato ", i, maxIntTomato.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Max GFP ", i, maxIntGFP.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Min Tomato ", i, minIntTomato.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Min GFP ", i, minIntGFP.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Mean Tomato ", i, meanIntTomatoL.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Mean GFP ", i, meanIntGFP.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Sum Tomato ", i, sumIntTomato.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("Mean Intensity Sum GFP ", i, sumIntGFP.stream()
//                .mapToDouble(d -> d)
//                .average()
//                .orElse(0.0).toString())
//        tablePerWell.setValue("N of GFP Class", i, nGFP.size().toString())
//        tablePerWell.setValue("N of Tomato Class", i, nTomato.size().toString())
//        tablePerWell.setValue("N of Full Label", i, nFull.size().toString())
//        tablePerWell.setValue("N of Empty Label", i, nEmpty.size().toString())
//        tablePerWell.saveAs(outputDir.getAbsolutePath() + File.separator + "csv" + File.separator + listOfFiles[i].getName() + "_perWell_table_results_erode.csv")
    }
}


static def calculateMedian(List<Double> arr) {
    def sortedArr = arr.sort()
    def n = sortedArr.size()

    if (n % 2 == 0) {
        // If even number of elements, take the average of the middle two
        def mid1 = sortedArr[n / 2 - 1]
        def mid2 = sortedArr[n / 2]
        return (mid1 + mid2) / 2
    } else {
        // If odd number of elements, take the middle element
        return sortedArr[n / 2]
    }
}

ArrayList<Roi> L2RA(ImagePlus imp) {
    ArrayList<Roi> roiArray = new ArrayList<>();
    ImageProcessor ip = imp.getProcessor();
    float[][] pixels = ip.getFloatArray();

    HashSet<Float> existingPixelValues = new HashSet<>();

    for (int x = 0; x < ip.getWidth(); x++) {
        for (int y = 0; y < ip.getHeight(); y++) {
            existingPixelValues.add((pixels[x][y]));
        }
    }

    // Converts data in case thats a RGB Image
    def fp = new FloatProcessor(ip.getWidth(), ip.getHeight())
    fp.setFloatArray(pixels)
    def imgFloatCopy = new ImagePlus("FloatLabel", fp)

    existingPixelValues.each { v ->
        fp.setThreshold(v, v, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = ThresholdToSelection.run(imgFloatCopy);
        roi.setName(Integer.toString((int) (double) v));
        roiArray.add(roi);
    }
    return roiArray;
}

ImagePlus extractCurrentStack(ImagePlus plus) {
    // check dimensionsnegativeGrayObjs
    int[] dims = plus.getDimensions();//XYCZT
    int channel = plus.getChannel();
    int frame = plus.getFrame();
    ImagePlus stack;
    // crop actual frame
    if ((dims[2] > 1) || (dims[4] > 1)) {
        IJ.log("hyperstack found, extracting current channel " + channel + " and frame " + frame);
        def duplicator = new Duplicator();
        stack = duplicator.run(plus, channel, channel, 1, dims[3], frame, frame);
    } else stack = plus.duplicate();

    return stack;
}

static double std(ArrayList<Double> table, double mean) {
    // Step 1:
    double meanDef = mean
    double temp = 0;

    for (int i = 0; i < table.size(); i++) {
        int val = table.get(i);

        // Step 2:
        double squrDiffToMean = Math.pow(val - meanDef, 2);

        // Step 3:
        temp += squrDiffToMean;
    }

    // Step 4:
    double meanOfDiffs = (double) temp / (double) (table.size());

    // Step 5:
    return Math.sqrt(meanOfDiffs);
}

