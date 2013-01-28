package at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser;

import org.apache.commons.lang.StringUtils;

class TextSegment {

    protected final int offset;
    protected final String segment;
    protected String processed;

    TextSegment(int offset, String segment){
        this.offset = offset;
        this.segment = segment;
    }
    
    @Override
    public String toString() {
        return new StringBuffer(StringUtils.abbreviate(segment, 10)).insert(0, ": ").insert(0,offset).toString();
    }
    @Override
    public int hashCode() {
        return segment.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof TextSegment && ((TextSegment)o).offset == offset &&
                ((TextSegment)o).segment.equals(segment);
    }
}
