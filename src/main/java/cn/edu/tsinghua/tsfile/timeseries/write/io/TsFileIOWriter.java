package cn.edu.tsinghua.tsfile.timeseries.write.io;

import cn.edu.tsinghua.tsfile.common.conf.TSFileDescriptor;
import cn.edu.tsinghua.tsfile.common.utils.BytesUtils;
import cn.edu.tsinghua.tsfile.common.utils.ListByteArrayOutputStream;
import cn.edu.tsinghua.tsfile.common.utils.TsRandomAccessFileWriter;
import cn.edu.tsinghua.tsfile.common.utils.ITsRandomAccessFileWriter;
import cn.edu.tsinghua.tsfile.file.metadata.*;
import cn.edu.tsinghua.tsfile.file.metadata.converter.TSFileMetaDataConverter;
import cn.edu.tsinghua.tsfile.file.metadata.enums.CompressionTypeName;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSChunkType;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.file.metadata.statistics.Statistics;
import cn.edu.tsinghua.tsfile.file.utils.ReadWriteThriftFormatUtils;
import cn.edu.tsinghua.tsfile.timeseries.write.desc.MeasurementDescriptor;
import cn.edu.tsinghua.tsfile.timeseries.write.schema.FileSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TSFileIOWriter is used to construct metadata and write data stored in memory to output stream.
 *
 * @author kangrong
 */
public class TsFileIOWriter {
  public static final String MAGIC_STRING = "TsFilev0.0.1";
  public static final byte[] magicStringBytes;
  public static final TSFileMetaDataConverter metadataConverter = new TSFileMetaDataConverter();
  private static final Logger LOG = LoggerFactory.getLogger(TsFileIOWriter.class);

  static {
    magicStringBytes = BytesUtils.StringToBytes(MAGIC_STRING);
  }

  private final ITsRandomAccessFileWriter out;
  protected List<RowGroupMetaData> rowGroupMetaDatas = new ArrayList<>();
  private RowGroupMetaData currentRowGroupMetaData;
  private TimeSeriesChunkMetaData currentChunkMetaData;

  /**
   * for writing a new tsfile.
   *
   * @param file
   *          be used to output written data
   * @throws IOException
   *           if I/O error occurs
   */
  public TsFileIOWriter(File file) throws IOException {
    this.out = new TsRandomAccessFileWriter(file);
    startFile();
  }

  /**
   * for writing a new tsfile.
   *
   * @param output
   *          be used to output written data
   * @throws IOException
   *           if I/O error occurs
   */
  public TsFileIOWriter(ITsRandomAccessFileWriter output) throws IOException {
    this.out = output;
    startFile();
  }

  /**
   * This is just used to restore one TSFile from List of RowGroupMetaData and the offset.
   *
   * @param output
   *          be used to output written data
   * @param offset
   *          offset to restore
   * @param rowGroups
   *          given a constructed row group list for fault recovery
   * @throws IOException
   *           if I/O error occurs
   */
  public TsFileIOWriter(ITsRandomAccessFileWriter output, long offset,
      List<RowGroupMetaData> rowGroups) throws IOException {
    this.out = output;
    out.seek(offset);
    this.rowGroupMetaDatas = rowGroups;
  }

  /**
   * Writes given <code>ListByteArrayOutputStream</code> to output stream. This method is called
   * when total memory size exceeds the row group size threshold.
   *
   * @param bytes
   *          - data of several pages which has been packed
   * @throws IOException
   *           if an I/O error occurs.
   */
  public void writeBytesToStream(ListByteArrayOutputStream bytes) throws IOException {
    bytes.writeAllTo(out.getOutputStream());
  }

  private void startFile() throws IOException {
    out.write(magicStringBytes);
  }

  /**
   * start a {@linkplain RowGroupMetaData RowGroupMetaData}.
   *
   * @param recordCount
   *          - the record count of this time series input in this stage
   * @param deltaObjectId
   *          - delta object id
   */
  public void startRowGroup(long recordCount, String deltaObjectId) {
    LOG.debug("start row group:{}", deltaObjectId);
    currentRowGroupMetaData = new RowGroupMetaData(deltaObjectId, recordCount, 0, new ArrayList<>(),
        "");// FIXME remove deltaType
  }

  /**
   * start a {@linkplain TimeSeriesChunkMetaData TimeSeriesChunkMetaData}.
   *
   * @param descriptor
   *          - measurement of this time series
   * @param compressionCodecName
   *          - compression name of this time series
   * @param tsDataType
   *          - data type
   * @param statistics
   *          - statistic of the whole series
   * @param maxTime
   *          - maximum timestamp of the whole series in this stage
   * @param minTime
   *          - minimum timestamp of the whole series in this stage
   * @throws IOException
   *           if I/O error occurs
   */
  public void startSeries(MeasurementDescriptor descriptor,
      CompressionTypeName compressionCodecName, TSDataType tsDataType, Statistics<?> statistics,
      long maxTime, long minTime) throws IOException {
    LOG.debug("start series:{}", descriptor);
    currentChunkMetaData = new TimeSeriesChunkMetaData(descriptor.getMeasurementId(),
        TSChunkType.VALUE, out.getPos(), compressionCodecName);
    TInTimeSeriesChunkMetaData t = new TInTimeSeriesChunkMetaData(tsDataType, minTime, maxTime);
    currentChunkMetaData.setTInTimeSeriesChunkMetaData(t);
    byte[] max = statistics.getMaxBytes();
    byte[] min = statistics.getMinBytes();

    VInTimeSeriesChunkMetaData v = new VInTimeSeriesChunkMetaData(tsDataType);
    TSDigest tsDigest = new TSDigest(ByteBuffer.wrap(max, 0, max.length),
        ByteBuffer.wrap(min, 0, min.length));
    v.setDigest(tsDigest);
    descriptor.setDataValues(v);
    currentChunkMetaData.setVInTimeSeriesChunkMetaData(v);
  }

  public void endSeries(long size, long totalValueCount) {
    LOG.debug("end series:{},totalvalue:{}", currentChunkMetaData, totalValueCount);
    currentChunkMetaData.setTotalByteSize(size);
    currentChunkMetaData.setNumRows(totalValueCount);
    currentRowGroupMetaData.addTimeSeriesChunkMetaData(currentChunkMetaData);
    currentChunkMetaData = null;
  }

  public void endRowGroup(long memSize) {
    currentRowGroupMetaData.setTotalByteSize(memSize);
    rowGroupMetaDatas.add(currentRowGroupMetaData);
    LOG.debug("end row group:{}", currentRowGroupMetaData);
    currentRowGroupMetaData = null;
  }

  /**
   * write {@linkplain TSFileMetaData TSFileMetaData} to output stream and close it.
   *
   * @throws IOException
   *           if I/O error occurs
   */
  public void endFile(FileSchema schema) throws IOException {
    List<TimeSeriesMetadata> timeSeriesList = schema.getTimeSeriesMetadatas();
    LOG.debug("get time series list:{}", timeSeriesList);
    TSFileMetaData tsfileMetadata = new TSFileMetaData(rowGroupMetaDatas, timeSeriesList,
        TSFileDescriptor.getInstance().getConfig().currentVersion);
    Map<String, String> props = schema.getProps();
    tsfileMetadata.setProps(props);
    serializeTsFileMetadata(tsfileMetadata);
    out.close();
    LOG.info("output stream is closed");
  }

  /**
   * get the length of normal OutputStream.
   *
   * @return - length of normal OutputStream
   * @throws IOException
   *           if I/O error occurs
   */
  public long getPos() throws IOException {
    return out.getPos();
  }

  private void serializeTsFileMetadata(TSFileMetaData footer) throws IOException {
    long footerIndex = out.getPos();
    LOG.debug("serialize the footer,file pos:{}", footerIndex);
    TSFileMetaDataConverter metadataConverter = new TSFileMetaDataConverter();
    ReadWriteThriftFormatUtils.writeFileMetaData(metadataConverter.toThriftFileMetadata(footer),
        out.getOutputStream());
    LOG.debug("serialize the footer finished, file pos:{}", out.getPos());
    out.write(BytesUtils.intToBytes((int) (out.getPos() - footerIndex)));
    out.write(magicStringBytes);
  }

  /**
   * fill in output stream to complete row group threshold.
   * @param diff how many bytes that will be filled.
   * @throws IOException if diff > Integer.max_value
   */
  public void fillInRowGroup(long diff) throws IOException {
    if (diff <= Integer.MAX_VALUE) {
      out.write(new byte[(int) diff]);
    } else {
      throw new IOException("write too much blank byte array!array size:" + diff);
    }
  }

  /**
   * Get the list of RowGroupMetaData in memory.
   *
   * @return - current list of RowGroupMetaData
   */
  public List<RowGroupMetaData> getRowGroups() {
    return rowGroupMetaDatas;
  }

}
