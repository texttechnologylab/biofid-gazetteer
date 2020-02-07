package org.biofid.gazetter.run;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import org.apache.commons.cli.*;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.biofid.gazetter.BIOfidTreeGazetteer;

import java.io.IOException;

/**
 * Created on 18.04.2019.
 */
public class TagTaxa {
	public static void main(String[] args) {
		
		Option inputOption = new Option("i", "input", true, "Input root path.");
		
		Option outputOption = new Option("o", "output", true, "Output path.");
		
		Option taxaOption = new Option("t", "taxa", true, "Taxa list path.");
		taxaOption.setArgs(Option.UNLIMITED_VALUES);
		
		Option minLen = new Option("m", "minlength", true, "Taxa minimum length. Default: 5.");
		minLen.setRequired(false);
		
		Options options = new Options();
		options.addOption("h", "help", false, "Print this message.");
		options.addOption(inputOption);
		options.addOption(outputOption);
		options.addOption(taxaOption);
		options.addOption(minLen);
		options.addOption("l", "lowercase", false, "Optional, if true use lowercase.");
		options.addOption("s", "allSkips", false, "Optional, if true use lowercase.");
		
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);
			
			if (cmd.hasOption("h")) {
				printUsage(options);
				return;
			}
			
			String inputLocation = cmd.getOptionValue("i");
			String[] taxaLocations = cmd.getOptionValues("t");
			String outputLocation = cmd.getOptionValue("o");
			Boolean useLowerCase = cmd.hasOption("l");
			Boolean getAllSkips = cmd.hasOption("s");
			Integer minLength = cmd.hasOption("m") ? Integer.valueOf(cmd.getOptionValue("m")) : 5;
			
			CollectionReader collection = CollectionReaderFactory.createReader(
					XmiReader.class,
					XmiReader.PARAM_PATTERNS, "[+]*.xmi",
					XmiReader.PARAM_SOURCE_LOCATION, inputLocation,
					XmiReader.PARAM_LENIENT, true
//						, XmiReader.PARAM_LOG_FREQ, -1
			);
			
			AggregateBuilder ab = new AggregateBuilder();
			ab.add(AnalysisEngineFactory.createEngineDescription(
					AnalysisEngineFactory.createEngineDescription(BIOfidTreeGazetteer.class,
							BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, taxaLocations,
							BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, useLowerCase,
							BIOfidTreeGazetteer.PARAM_MIN_LENGTH, minLength,
							BIOfidTreeGazetteer.PARAM_GET_ALL_SKIPS, getAllSkips)
			));
			ab.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
					XmiWriter.PARAM_TARGET_LOCATION, outputLocation,
					XmiWriter.PARAM_OVERWRITE, true
			));
			
			SimplePipeline.runPipeline(collection, ab.createAggregate());

//			AtomicInteger count = new AtomicInteger(0);
//			Stream<File> fileStream = Stream.empty();
//			for (String inputLocation : inputLocations) {
//				fileStream = Streams.concat(fileStream,
//						Streams.stream(Files.fileTraverser().breadthFirst(new File(inputLocation)))
//								.filter(File::isFile));
//			}
//			File[] files = fileStream.sorted(Comparator.comparing(FileUtils::sizeOf)).toArray(File[]::new);
//			int allCount = files.length;
//			for (File file : files) {
//				// TODO: does this work?
//				Path outFilePath = Paths.get(outputLocation, file.getName());
//				try (FileLock fileLock = FileChannel.open(outFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW).tryLock()) {
//					if (fileLock == null || !fileLock.isValid()) {
//						System.out.printf("\rSkipped file %0" + (int) (log10(allCount) + 1) + "d/%d", count.incrementAndGet(), allCount);
//						continue;
//					}
//
//					System.out.printf("\rRunning file %0" + (int) (log10(allCount) + 1) + "d/%d", count.incrementAndGet(), allCount);
//					JCas jCas = JCasFactory.createJCas();
//					CasIOUtils.load(java.nio.file.Files.newInputStream(file.toPath().toAbsolutePath()), null, jCas.getCas(), true);
//
//					SimplePipeline.runPipeline(jCas, analysisEngine);
//					XmiCasSerializer.serialize(jCas.getCas(), new FileOutputStream(outFilePath.toFile()));
//				} catch (FileAlreadyExistsException | OverlappingFileLockException | ClosedChannelException e) {
//					System.out.printf("\rSkipped file %0" + (int) (log10(allCount) + 1) + "d/%d", count.incrementAndGet(), allCount);
//				} catch (UIMAException | UIMARuntimeException | SAXException | IOException | StringIndexOutOfBoundsException e) {
//					System.err.printf("\rAn error occurred while writing to '%s', deleting file..\n", outFilePath.toString());
//					outFilePath.toFile().delete();
//					e.printStackTrace();
//				}
//			}
			System.out.println("\nDone.");
		} catch (ParseException | UIMAException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -cp $CP org.hucompute.textimager.biofid.TagTaxa",
				"TODO", //TODO
				options,
				"",
				true);
	}
}
