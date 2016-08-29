package studio.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.ToDouble;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.util.TimeZone;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.util.ShapeUtilities;
import studio.kdb.Config;

public class LineChart {
    public ChartPanel chartPanel;
    JFrame frame = null;

    private static int[] intArray(double a, double b, double c, double d) {
        return new int[] {(int) a, (int) b, (int) c, (int) d};
    }
    private static int[] intArray(double a, double b, double c) {
        return new int[] {(int) a, (int) b, (int) c};
    }
    
    public static Shape[] createStandardSeriesShapes() {

        Shape[] result = new Shape[10];

        double size = 3.0;
        double delta = size / 2.0;
        int[] xpoints;
        int[] ypoints;

        // square
        result[0] = new Rectangle2D.Double(-delta, -delta, size, size);
        // circle
        result[1] = new Ellipse2D.Double(-delta, -delta, size, size);

        // up-pointing triangle
        xpoints = intArray(0.0, delta, -delta);
        ypoints = intArray(-delta, delta, delta);
        result[2] = new Polygon(xpoints, ypoints, 3);

        // cross
        result[3] = ShapeUtilities.createDiagonalCross((float)delta, 0.1f);

        // horizontal rectangle
        result[4] = new Rectangle2D.Double(-delta, -delta / 2, size, size / 2);

        // down-pointing triangle
        xpoints = intArray(-delta, +delta, 0.0);
        ypoints = intArray(-delta, -delta, delta);
        result[5] = new Polygon(xpoints, ypoints, 3);

        // horizontal ellipse
        result[6] = new Ellipse2D.Double(-delta, -delta / 2, size, size / 2);

        // right-pointing triangle
        xpoints = intArray(-delta, delta, -delta);
        ypoints = intArray(-delta, 0.0, delta);
        result[7] = new Polygon(xpoints, ypoints, 3);

        // vertical rectangle
        result[8] = new Rectangle2D.Double(-delta / 2, -delta, size / 2, size);

        // left-pointing triangle
        xpoints = intArray(-delta, delta, delta);
        ypoints = intArray(0.0, -delta, +delta);
        result[9] = new Polygon(xpoints, ypoints, 3);

        return result;
    }
    
    public LineChart(KTableModel table) {
        JFreeChart chart = createDataset(table);
        if (chart != null) {
            StandardChartTheme theme = (StandardChartTheme)org.jfree.chart.StandardChartTheme.createDarknessTheme();

            String fontName = "Lucida Sans";
            theme.setTitlePaint( Color.decode( "#4572a7" ) );
            theme.setExtraLargeFont( new Font(fontName,Font.PLAIN, 16) ); //title
            theme.setLargeFont( new Font(fontName,Font.BOLD, 15)); //axis-title
            theme.setRegularFont( new Font(fontName,Font.PLAIN, 11));
            theme.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[]{
                    Color.decode("0xcd7573"), Color.decode("0x789fcc"), 
                    Color.decode("0xb2cb7f"), Color.decode("0x9b85b6"), 
                    Color.decode("0xFFA856"), Color.decode("0x3FBFE3"), 
                    Color.decode("0xFFFF7F"), Color.decode("0x6681CC"),
                    Color.decode("0xFF7F7F"), Color.decode("0xFFFFBF"),
                    Color.decode("0x99A6CC"), Color.decode("0xFFBFBF"),
                    Color.decode("0xA9A938"), Color.decode("0x2D4587")},
                    new Paint[] {Color.decode("0xFFFF00"),
                        Color.decode("0x0036CC")},
                    new Stroke[] {new BasicStroke(1.0f)},
                    new Stroke[] {new BasicStroke(0.25f)},
                    createStandardSeriesShapes()));
            theme.apply( chart );
            chart.setTextAntiAlias( true );
            Plot plot = chart.getPlot();
            if (plot instanceof XYPlot) {
                XYPlot xyPlot = chart.getXYPlot();
                XYStepRenderer xyir = new XYStepRenderer() {
                    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, 
                            XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, 
                            CrosshairState crosshairState, int pass) {
                        if (isLinePass(pass)) {
                             super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
                        } else if(isItemPass(pass)){
                            // setup for collecting optional entity info...
                            EntityCollection entities = null;
                            if (info != null) {
                                entities = info.getOwner().getEntityCollection();
                            }

                            drawSecondaryPass(g2, plot, dataset, pass, series, item,
                                    domainAxis, dataArea, rangeAxis, crosshairState, entities);
                        }

                    }
                };
                xyir.setBaseShapesVisible(true);
                xyir.setShapesVisible(true);
                xyPlot.setDomainGridlinePaint(Color.DARK_GRAY);
                xyPlot.setRangeGridlinePaint(Color.gray);
                xyPlot.setDomainPannable(true);
                xyPlot.setRangePannable(true);
                xyPlot.setRenderer(xyir);
                /*try {
                    for (int i = 0; i < xyPlot.getSeriesCount(); i++) {
                        Shape shape = xyir.getSeriesShape(i);
                        xyir.setSeriesShape(i, shape);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
            frame = new JFrame("Studio for kdb+ [chart]");

            chartPanel = new ChartPanel(chart);
            chartPanel.setAutoscrolls(true);
            chartPanel.setMouseWheelEnabled(true);
            
            frame.setContentPane(chartPanel);
            JLabel tooltips = new JLabel("Press and hold CTRL key to pan");
            tooltips.setFont(new Font(fontName,Font.PLAIN, 9));
            chartPanel.add(tooltips);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            frame.setIconImage(Util.getImage(Config.imageBase2 + "chart_24.png").getImage());

            frame.pack();
            frame.setVisible(true);
            frame.requestFocus();
            frame.toFront();
        }
    }

    public static JFreeChart createDataset(KTableModel table) {
        TimeZone tz = TimeZone.getTimeZone("GMT");

        XYDataset ds = null;

        if (table.getColumnCount() > 0) {
            Class klass = table.getColumnClass(0);

            if ((klass == K.KTimestampVector.class) ||(klass == K.KTimespanVector.class) || (klass == K.KDateVector.class) || (klass == K.KTimeVector.class) || (klass == K.KMonthVector.class) || (klass == K.KMinuteVector.class) || (klass == K.KSecondVector.class) || (klass == K.KDatetimeVector.class)) {
                TimeSeriesCollection tsc = new TimeSeriesCollection(tz);

                for (int col = 1;col < table.getColumnCount();col++) {
                    TimeSeries series = null;

                    try {
                        if (klass == K.KDateVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Day.class);
                            K.KDateVector dates = (K.KDateVector) table.getColumn(0);

                            for (int row = 0;row < dates.getLength();row++) {
                                K.KDate date = (K.KDate) dates.at(row);
                                Day day = new Day(date.toDate(),tz);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(day,((ToDouble) o).toDouble());
                            }
                        }
                        else if (klass == K.KTimeVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Millisecond.class);

                            K.KTimeVector times = (K.KTimeVector) table.getColumn(0);
                            for (int row = 0;row < table.getRowCount();row++) {
                                K.KTime time = (K.KTime) times.at(row);
                                Millisecond ms = new Millisecond(time.toTime(),tz);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(ms,((ToDouble) o).toDouble());
                            }
                        }
                        else if (klass == K.KTimestampVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Day.class);
                            K.KTimestampVector dates = (K.KTimestampVector) table.getColumn(0);

                            for (int row = 0;row < dates.getLength();row++) {
                                K.KTimestamp date = (K.KTimestamp) dates.at(row);
                                Day day = new Day(new java.util.Date(date.toTimestamp().getTime()),tz);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(day,((ToDouble) o).toDouble());
                            }
                        }
                        else if (klass == K.KTimespanVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Millisecond.class);

                            K.KTimespanVector times = (K.KTimespanVector) table.getColumn(0);
                            for (int row = 0;row < table.getRowCount();row++) {
                                K.KTimespan time = (K.KTimespan) times.at(row);
                                Millisecond ms = new Millisecond(time.toTime(),tz);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(ms,((ToDouble) o).toDouble());
                            }
                        }
                        else if (klass == K.KDatetimeVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Millisecond.class);
                            K.KDatetimeVector times = (K.KDatetimeVector) table.getColumn(0);

                            for (int row = 0;row < table.getRowCount();row++) {
                                K.KDatetime time = (K.KDatetime) times.at(row);
                                Millisecond ms = new Millisecond(time.toTimestamp(),tz);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(ms,((ToDouble) o).toDouble());
                            }
                        }
                        else if (klass == K.KMonthVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Month.class);
                            K.KMonthVector times = (K.KMonthVector) table.getColumn(0);
                            for (int row = 0;row < table.getRowCount();row++) {
                                K.Month time = (K.Month) times.at(row);
                                int m = time.i + 24000;
                                int y = m / 12;
                                m = 1 + m % 12;

                                Month month = new Month(m,y);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(month,((ToDouble) o).toDouble());
                            }
                        }
                        else if (klass == K.KSecondVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Second.class);
                            K.KSecondVector times = (K.KSecondVector) table.getColumn(0);
                            for (int row = 0;row < table.getRowCount();row++) {
                                K.Second time = (K.Second) times.at(row);
                                Second second = new Second(time.i % 60,time.i / 60,0,1,1,2001);

                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(second,((ToDouble) o).toDouble());

                            }
                        }
                        else if (klass == K.KMinuteVector.class) {
                            series = new TimeSeries(table.getColumnName(col),Minute.class);
                            K.KMinuteVector times = (K.KMinuteVector) table.getColumn(0);
                            for (int row = 0;row < table.getRowCount();row++) {
                                K.Minute time = (K.Minute) times.at(row);
                                Minute minute = new Minute(time.i % 60,time.i / 60,1,1,2001);
                                Object o = table.getValueAt(row,col);
                                if (o instanceof K.KBase)
                                    if (!((K.KBase) o).isNull())
                                        if (o instanceof ToDouble)
                                            series.addOrUpdate(minute,((ToDouble) o).toDouble());
                            }
                        }
                    }
                    catch (SeriesException e) {
                        System.err.println("Error adding to series");
                    }


                    if (series.getItemCount() > 0)
                        tsc.addSeries(series);
                }

                ds = tsc;
            }
            else if ((klass == K.KDoubleVector.class) || (klass == K.KFloatVector.class) || (klass == K.KShortVector.class) || (klass == K.KIntVector.class) || (klass == K.KLongVector.class)) {
                XYSeriesCollection xysc = new XYSeriesCollection();

                for (int col = 1;col < table.getColumnCount();col++) {
                    XYSeries series = null;

                    try {
                        series = new XYSeries(table.getColumnName(col));

                        for (int row = 0;row < table.getRowCount();row++) {
                            double x = ((ToDouble) table.getValueAt(row,0)).toDouble();
                            double y = ((ToDouble) table.getValueAt(row,col)).toDouble();
                            series.add(x,y);
                        }
                    }
                    catch (SeriesException e) {
                        System.err.println("Error adding to series");
                    }

                    if (series.getItemCount() > 0)
                        xysc.addSeries(series);
                }

                ds = xysc;
            }
        }

        if (ds != null) {
            boolean legend = false;

            if (ds.getSeriesCount() > 1)
                legend = true;

            if (ds instanceof XYSeriesCollection)
                return ChartFactory.createXYLineChart("",
                                                      "",
                                                      "",
                                                      ds,
                                                      PlotOrientation.VERTICAL,
                                                      legend,
                                                      true,
                                                      true);
            else if (ds instanceof TimeSeriesCollection)
                return ChartFactory.createTimeSeriesChart("",
                                                      "",
                                                      "",
                                                      ds,
                                                      legend,
                                                      true,
                                                      true);
        }

        return null;
    }
}

