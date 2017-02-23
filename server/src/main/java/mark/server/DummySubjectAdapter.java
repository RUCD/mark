/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package mark.server;

import com.fasterxml.jackson.databind.JsonNode;
import mark.core.SubjectAdapter;
import org.bson.Document;

/**
 *
 * @author Thibault Debatty
 */
public class DummySubjectAdapter implements SubjectAdapter<DummySuject> {

    @Override
    public void writeToMongo(DummySuject subject, Document doc) {
        doc.append("name", subject.name);
    }

    @Override
    public DummySuject readFromMongo(Document doc) {
        DummySuject ds = new DummySuject();
        ds.name = doc.getString("name");
        return ds;
    }

    @Override
    public DummySuject deserialize(JsonNode node) {
        DummySuject ds = new DummySuject();
        ds.name = node.get("name").toString();
        return ds;
    }
}
