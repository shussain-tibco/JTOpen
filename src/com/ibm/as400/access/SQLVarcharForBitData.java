///////////////////////////////////////////////////////////////////////////////
//                                                                             
// JTOpen (IBM Toolbox for Java - OSS version)                                 
//                                                                             
// Filename: SQLVarcharForBitData.java
//                                                                             
// The source code contained herein is licensed under the IBM Public License   
// Version 1.0, which has been approved by the Open Source Initiative.         
// Copyright (C) 1997-2003 International Business Machines Corporation and     
// others. All rights reserved.                                                
//                                                                             
///////////////////////////////////////////////////////////////////////////////

package com.ibm.as400.access;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

class SQLVarcharForBitData
implements SQLData
{
    private static final String copyright = "Copyright (C) 1997-2003 International Business Machines Corporation and others.";

    // Private data.
    private static final byte[]     default_    = new byte[0]; // @C2A

    private SQLConversionSettings   settings_;
    private int                     length_;
    private boolean                 longValue_;
    private int                     maxLength_;
    private int                     truncated_;
    private byte[]                  value_;

    SQLVarcharForBitData(int maxLength,
                         boolean longValue,
                         SQLConversionSettings settings)
    {
        settings_       = settings;
        length_         = 0;
        longValue_      = longValue;
        maxLength_      = maxLength;
        truncated_      = 0;
        value_          = default_; // @C2C
    }

    public Object clone()
    {
        return new SQLVarcharForBitData(maxLength_, longValue_, settings_);
    }

    //---------------------------------------------------------//
    //                                                         //
    // CONVERSION TO AND FROM RAW BYTES                        //
    //                                                         //
    //---------------------------------------------------------//

    public void convertFromRawBytes(byte[] rawBytes, int offset, ConvTable ccsidConverter) //@P0C
    throws SQLException
    {
        length_ = BinaryConverter.byteArrayToUnsignedShort(rawBytes, offset);
        AS400ByteArray typeConverter = new AS400ByteArray(length_);
        value_ = (byte[])typeConverter.toObject(rawBytes, offset+2);
    }

    public void convertToRawBytes(byte[] rawBytes, int offset, ConvTable ccsidConverter) //@P0C
    throws SQLException
    {
        AS400ByteArray typeConverter = new AS400ByteArray(length_);
        BinaryConverter.unsignedShortToByteArray(length_, rawBytes, offset);
        typeConverter.toBytes(value_, rawBytes, offset + 2);
    }

    //---------------------------------------------------------//
    //                                                         //
    // SET METHODS                                             //
    //                                                         //
    //---------------------------------------------------------//

    public void set(Object object, Calendar calendar, int scale)
    throws SQLException
    {
        if(object instanceof String)
            value_ = SQLBinary.stringToBytes((String)object); //@F1A

        else if(object instanceof byte[])
            value_ = (byte[])object;                                                 // @C1C

        else if(object instanceof InputStream)
            value_ = JDUtilities.streamToBytes((InputStream)object, scale);

        else if(object instanceof Reader)
            value_ = SQLBinary.stringToBytes(JDUtilities.readerToString((Reader)object, scale));

        else if(JDUtilities.JDBCLevel_ >= 20 && object instanceof Blob)
            value_ = ((Blob)object).getBytes(1, (int)((Blob)object).length());      // @C1C @E2C Blobs are 1 based.

        else if(JDUtilities.JDBCLevel_ >= 20 && object instanceof Clob)
            value_ = SQLBinary.stringToBytes(((Clob)object).getSubString(1, (int)((Clob)object).length())); //@F1A

        else
            JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);

        // Truncate if necessary.
        int valueLength = value_.length;
        if(valueLength > maxLength_)
        {
            byte[] newValue = new byte[maxLength_];
            System.arraycopy(value_, 0, newValue, 0, maxLength_);
            value_ = newValue;
            truncated_ = valueLength - maxLength_;
        }
        else
            truncated_ = 0;

        length_ = value_.length;
    }

    //---------------------------------------------------------//
    //                                                         //
    // DESCRIPTION OF SQL TYPE                                 //
    //                                                         //
    //---------------------------------------------------------//

    public String getCreateParameters()
    {
        return AS400JDBCDriver.getResource("MAXLENGTH");
    }

    public int getDisplaySize()
    {
        return maxLength_ ;
    }

    //@F2A JDBC 3.0
    public String getJavaClassName()
    {
        return "[B";
    }

    public String getLiteralPrefix()
    {
        return "X\'";
    }

    public String getLiteralSuffix()
    {
        return "\'";
    }

    public String getLocalName()
    {
        return "VARCHAR";
    }

    public int getMaximumPrecision()
    {
        return 32739;
    }

    public int getMaximumScale()
    {
        return 0;
    }

    public int getMinimumScale()
    {
        return 0;
    }

    public int getNativeType()
    {
        return(longValue_ ? 456 : 448);
    }

    public int getPrecision()
    {
        return maxLength_;
    }

    public int getRadix()
    {
        return 0;
    }

    public int getScale()
    {
        return 0;
    }

    public int getType()
    {
        return java.sql.Types.VARBINARY;
    }

    public String getTypeName()
    {
        return "VARCHAR() FOR BIT DATA";
    }

    public boolean isSigned()
    {
        return false;
    }

    public boolean isText()
    {
        return true;
    }

    public int getActualSize()
    {
        return value_.length;
    }

    public int getTruncated()
    {
        return truncated_;
    }

    //---------------------------------------------------------//
    //                                                         //
    // CONVERSIONS TO JAVA TYPES                               //
    //                                                         //
    //---------------------------------------------------------//

    public InputStream toAsciiStream()
    throws SQLException
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        
        // changed to return stream containing hex string
        // return new ByteArrayInputStream(toBytes());
        try
        {
            return new ByteArrayInputStream(ConvTable.getTable(819, null).stringToByteArray(SQLBinary.bytesToString(toBytes())));
        }
        catch(UnsupportedEncodingException e)
        {
            JDError.throwSQLException(this, JDError.EXC_INTERNAL, e);
            return null;
        }
    }

    public BigDecimal toBigDecimal(int scale)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public InputStream toBinaryStream()
    throws SQLException
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        return new ByteArrayInputStream(toBytes());
    }

    public Blob toBlob()
    throws SQLException
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        return new AS400JDBCBlob(toBytes(), maxLength_);
    }

    public boolean toBoolean()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return false;
    }

    public byte toByte()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public byte[] toBytes()
    {
        // Truncate to the max field size if needed.
        // Do not signal a DataTruncation per the spec. @B1A
        int maxFieldSize = settings_.getMaxFieldSize();
        if((value_.length > maxFieldSize) && (maxFieldSize > 0))
        {
            // @B1D truncated_ = value_.length - maxFieldSize;
            byte[] truncatedValue = new byte[maxFieldSize];
            System.arraycopy(value_, 0, truncatedValue, 0, maxFieldSize);
            return truncatedValue;
        }
        else
        {
            // @B1D truncated_ = 0;
            return value_;
        }
    }

    public Reader toCharacterStream()
    throws SQLException
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        //@F1D return new StringReader(new String(toBytes()));
        return new StringReader(SQLBinary.bytesToString(toBytes())); //@F1A
    }

    public Clob toClob()
    throws SQLException
    {
        // This is written in terms of toString(), since it will
        // handle truncating to the max field size if needed.
        //@F1D return new AS400JDBCClob(new String(toBytes()));
        return new AS400JDBCClob(SQLBinary.bytesToString(toBytes()), maxLength_); //@F1A
    }

    public Date toDate(Calendar calendar)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public double toDouble()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public float toFloat()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public int toInt()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public long toLong()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public Object toObject()
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        return toBytes();
    }

    public short toShort()
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return -1;
    }

    public String toString()
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.
        //@F1D return new String(toBytes());
        return SQLBinary.bytesToString(toBytes()); //@F1A
    }

    public Time toTime(Calendar calendar)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public Timestamp toTimestamp(Calendar calendar)
    throws SQLException
    {
        JDError.throwSQLException(this, JDError.EXC_DATA_TYPE_MISMATCH);
        return null;
    }

    public InputStream toUnicodeStream()
    throws SQLException
    {
        // This is written in terms of toBytes(), since it will
        // handle truncating to the max field size if needed.

        // changed to return stream containing hex string
        // return new ByteArrayInputStream(toBytes());
        try
        {
            return new ByteArrayInputStream(ConvTable.getTable(13488, null).stringToByteArray(SQLBinary.bytesToString(toBytes())));
        }
        catch(UnsupportedEncodingException e)
        {
            JDError.throwSQLException(this, JDError.EXC_INTERNAL, e);
            return null;
        }
    }
}

