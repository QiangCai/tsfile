/**
 * Autogenerated by Thrift Compiler (0.9.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.corp.delta.tsfile.format;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

/**
 * Types supported by TSFile.  These types are intended to be used in combination
 * with the encodings to control the on disk storage format.
 * For example INT16 is not included as a type since a good encoding of INT32
 * would handle this.
 */
public enum DataType implements org.apache.thrift.TEnum {
  BOOLEAN(0),
  INT32(1),
  INT64(2),
  INT96(3),
  FLOAT(4),
  DOUBLE(5),
  BYTE_ARRAY(6),
  FIXED_LEN_BYTE_ARRAY(7),
  ENUMS(8),
  BIGDECIMAL(9);

  private final int value;

  private DataType(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static DataType findByValue(int value) { 
    switch (value) {
      case 0:
        return BOOLEAN;
      case 1:
        return INT32;
      case 2:
        return INT64;
      case 3:
        return INT96;
      case 4:
        return FLOAT;
      case 5:
        return DOUBLE;
      case 6:
        return BYTE_ARRAY;
      case 7:
        return FIXED_LEN_BYTE_ARRAY;
      case 8:
        return ENUMS;
      case 9:
        return BIGDECIMAL;
      default:
        return null;
    }
  }
}
