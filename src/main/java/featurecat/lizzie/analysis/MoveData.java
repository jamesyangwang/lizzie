package featurecat.lizzie.analysis;

import featurecat.lizzie.Lizzie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Holds the data from Leelaz's pondering mode */
public class MoveData {
  public String coordinate;
  public int playouts;
  public double winrate;
  public List<String> variation;

  private MoveData() {}

  /**
   * Parses a leelaz ponder output line. For example:
   *
   * <p>0.16 0.15
   *
   * <p>info move R5 visits 38 winrate 5404 order 0 pv R5 Q5 R6 S4 Q10 C3 D3 C4 C6 C5 D5
   *
   * <p>0.17
   *
   * <p>info move Q16 visits 80 winrate 4405 prior 1828 lcb 4379 order 0 pv Q16 D4
   *
   * @param line line of ponder output
   */
  public static MoveData fromInfo(String line) throws ArrayIndexOutOfBoundsException {
    MoveData result = new MoveData();
    String[] data = line.trim().split(" ");
    boolean islcb = Lizzie.config.showLcbWinrate;
    // Todo: Proper tag parsing in case gtp protocol is extended(?)/changed
    for (int i = 0; i < data.length; i++) {
      String key = data[i];
      if (key.equals("pv")) {
        // Read variation to the end of line
        result.variation = new ArrayList<>(Arrays.asList(data));
        result.variation = result.variation.subList(i + 1, data.length);
        break;
      } else {
        String value = data[++i];
        if (key.equals("move")) {
          result.coordinate = value;
        }
        if (key.equals("visits")) {
          result.playouts = Integer.parseInt(value);
        }
        if (islcb && key.equals("lcb")) {
          // LCB support
          result.winrate = Integer.parseInt(value) / 100.0;
        }

        if (key.equals("winrate")) {
          // support 0.16 0.15
          result.winrate = Integer.parseInt(value) / 100.0;
        }
      }
    }
    return result;
  }

  /**
   * Parses a leelaz summary output line. For example:
   *
   * <p>0.15 0.16
   *
   * <p>P16 -> 4 (V: 50.94%) (N: 5.79%) PV: P16 N18 R5 Q5
   *
   * <p>0.17
   *
   * <p>Q4 -> 4348 (V: 43.88%) (LCB: 43.81%) (N: 18.67%) PV: Q4 D16 D4 Q16 R14 R6 C1
   *
   * @param summary line of summary output
   */
  public static MoveData fromSummary(String summary) {
    Matcher match = summaryPatternLcb.matcher(summary.trim());
    if (!match.matches()) {
      // support 0.16 0.15
      Matcher matchold = summaryPatternWinrate.matcher(summary.trim());
      if (!matchold.matches()) {
        throw new IllegalArgumentException("Unexpected summary format: " + summary);
      } else {
        MoveData result = new MoveData();
        result.coordinate = matchold.group(1);
        result.playouts = Integer.parseInt(matchold.group(2));
        result.winrate = Double.parseDouble(matchold.group(3));
        result.variation = Arrays.asList(matchold.group(4).split(" "));
        return result;
      }
    } else {
      MoveData result = new MoveData();
      result.coordinate = match.group(1);
      result.playouts = Integer.parseInt(match.group(2));
      result.winrate = Double.parseDouble(match.group(Lizzie.config.showLcbWinrate ? 4 : 3));
      result.variation = Arrays.asList(match.group(5).split(" "));
      return result;
    }
  }

  private static Pattern summaryPatternLcb =
      Pattern.compile(
          "^ *(\\w\\d*) -> *(\\d+) \\(V: ([^%)]+)%\\) \\(LCB: ([^%)]+)%\\) \\([^\\)]+\\) PV: (.+).*$");
  private static Pattern summaryPatternWinrate =
      Pattern.compile("^ *(\\w\\d*) -> *(\\d+) \\(V: ([^%)]+)%\\) \\([^\\)]+\\) PV: (.+).*$");
  // support 0.16 0.15

  public static int getPlayouts(List<MoveData> moves) {
    int playouts = 0;
    for (MoveData move : moves) {
      playouts += move.playouts;
    }
    return playouts;
  }
}
