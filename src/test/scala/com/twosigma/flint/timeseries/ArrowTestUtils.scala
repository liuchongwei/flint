/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twosigma.flint.timeseries

import com.twosigma.flint.arrow.ArrowUtils
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.file.ArrowFileReader
import org.apache.arrow.vector.util.ByteArrayReadableSeekableByteChannel
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema

import scala.collection.JavaConverters._

object ArrowTestUtils {
  def fileFormatToRows(bytes: Array[Byte]): Seq[Row] = {
    val allocator = new RootAllocator(Int.MaxValue)
    val channel = new ByteArrayReadableSeekableByteChannel(bytes)
    val reader = new ArrowFileReader(channel, allocator)

    val root = reader.getVectorSchemaRoot
    val schema = ArrowUtils.fromArrowSchema(root.getSchema)
    reader.loadNextBatch()
    val vectors = root.getFieldVectors.asScala

    val rowCount = root.getRowCount
    val columnCount = root.getSchema.getFields.size()

    val values = (0 until rowCount).map { i =>
      (0 until columnCount).map{ j =>
        vectors(j).getAccessor.getObject(i)
      }
    }

    val rows = values.map { value =>
      new GenericRowWithSchema(value.toArray, schema)
    }

    reader.close()
    root.close()
    allocator.close()

    rows
  }
}
