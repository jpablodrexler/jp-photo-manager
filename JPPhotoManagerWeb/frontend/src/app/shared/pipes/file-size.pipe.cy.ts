import { FileSizePipe } from './file-size.pipe';

describe('FileSizePipe', () => {
  const pipe = new FileSizePipe();

  it('should return "0 B" for zero bytes', () => {
    expect(pipe.transform(0)).to.equal('0 B');
  });

  it('should format bytes', () => {
    expect(pipe.transform(512)).to.equal('512.0 B');
  });

  it('should format kilobytes', () => {
    expect(pipe.transform(1024)).to.equal('1.0 KB');
  });

  it('should format megabytes', () => {
    expect(pipe.transform(1048576)).to.equal('1.0 MB');
  });

  it('should format gigabytes', () => {
    expect(pipe.transform(1073741824)).to.equal('1.0 GB');
  });

  it('should format terabytes', () => {
    expect(pipe.transform(1099511627776)).to.equal('1.0 TB');
  });
});
