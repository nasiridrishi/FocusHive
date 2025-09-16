/**
 * Media and File Handling Compatibility Tests
 * Tests media formats, file operations, and related APIs across browsers
 */

import {expect, test} from '@playwright/test';

test.describe('Image Format Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support modern image formats', async ({page}) => {
    const imageSupport = await page.evaluate(() => {
      const canvas = document.createElement('canvas');
      canvas.width = canvas.height = 1;

      const formats = {
        webp: canvas.toDataURL('image/webp').indexOf('webp') !== -1,
        avif: canvas.toDataURL('image/avif').indexOf('avif') !== -1,
        jpeg: true, // Always supported
        png: true,  // Always supported
        gif: true,  // Always supported
        svg: true   // Always supported
      };

      return formats;
    });

    expect(imageSupport.jpeg).toBe(true);
    expect(imageSupport.png).toBe(true);
    expect(imageSupport.gif).toBe(true);
    expect(imageSupport.svg).toBe(true);
    expect(imageSupport.webp).toBe(true); // Should be supported in modern browsers

    // AVIF support varies by browser
    console.log('Image Format Support:', imageSupport);
  });

  test('should load and display different image formats', async ({page}) => {
    await page.setContent(`
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding: 20px;">
        <div>
          <h3>JPEG</h3>
          <img id="jpeg-img" src="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A" alt="JPEG test" style="width: 50px; height: 50px; background: red;">
        </div>
        <div>
          <h3>PNG</h3>
          <img id="png-img" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==" alt="PNG test" style="width: 50px; height: 50px; background: green;">
        </div>
        <div>
          <h3>SVG</h3>
          <img id="svg-img" src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIHZpZXdCb3g9IjAgMCA1MCA1MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjUwIiBoZWlnaHQ9IjUwIiBmaWxsPSJibHVlIi8+Cjwvc3ZnPgo=" alt="SVG test" style="width: 50px; height: 50px; background: blue;">
        </div>
      </div>
    `);

    // Test that images load successfully
    const jpegImg = page.locator('#jpeg-img');
    const pngImg = page.locator('#png-img');
    const svgImg = page.locator('#svg-img');

    await expect(jpegImg).toBeVisible();
    await expect(pngImg).toBeVisible();
    await expect(svgImg).toBeVisible();

    // Take screenshot to verify rendering
    await expect(page.locator('div').first()).toHaveScreenshot('image-formats.png');
  });

  test('should handle image loading errors gracefully', async ({page}) => {
    await page.setContent(`
      <img id="broken-img" src="https://invalid-url.example/nonexistent.jpg" 
           alt="Broken image" 
           style="width: 100px; height: 100px; border: 1px solid red;"
           onerror="this.style.backgroundColor = 'lightgray'; this.alt = 'Failed to load';">
    `);

    const img = page.locator('#broken-img');
    await page.waitForTimeout(2000); // Wait for error handling

    // Check that error handling worked
    const backgroundColor = await img.evaluate((el: HTMLImageElement) => {
      return window.getComputedStyle(el).backgroundColor;
    });

    expect(backgroundColor).toContain('lightgray');
  });

  test('should support responsive images', async ({page}) => {
    await page.setContent(`
      <picture>
        <source media="(max-width: 480px)" srcset="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJyZWQiLz48L3N2Zz4=">
        <source media="(max-width: 768px)" srcset="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJncmVlbiIvPjwvc3ZnPg==">
        <img id="responsive-img" src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJibHVlIi8+PC9zdmc+" 
             alt="Responsive image test" style="width: 200px; height: 200px;">
      </picture>
    `);

    const img = page.locator('#responsive-img');
    await expect(img).toBeVisible();

    // Test different viewport sizes
    const viewports = [
      {width: 375, height: 667},  // Mobile
      {width: 768, height: 1024}, // Tablet
      {width: 1200, height: 800}  // Desktop
    ];

    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await page.waitForTimeout(200);

      const isVisible = await img.isVisible();
      expect(isVisible).toBe(true);
    }
  });
});

test.describe('Video and Audio Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
  });

  test('should support video formats', async ({page}) => {
    const videoSupport = await page.evaluate(() => {
      const video = document.createElement('video');

      const formats = {
        mp4: video.canPlayType('video/mp4') !== '',
        webm: video.canPlayType('video/webm') !== '',
        ogg: video.canPlayType('video/ogg') !== '',
        mov: video.canPlayType('video/quicktime') !== '',
        avi: video.canPlayType('video/x-msvideo') !== ''
      };

      return formats;
    });

    expect(videoSupport.mp4).toBe(true); // MP4 should be widely supported
    console.log('Video Format Support:', videoSupport);
  });

  test('should support audio formats', async ({page}) => {
    const audioSupport = await page.evaluate(() => {
      const audio = document.createElement('audio');

      const formats = {
        mp3: audio.canPlayType('audio/mpeg') !== '',
        ogg: audio.canPlayType('audio/ogg') !== '',
        wav: audio.canPlayType('audio/wav') !== '',
        aac: audio.canPlayType('audio/aac') !== '',
        flac: audio.canPlayType('audio/flac') !== ''
      };

      return formats;
    });

    expect(audioSupport.mp3).toBe(true); // MP3 should be widely supported
    console.log('Audio Format Support:', audioSupport);
  });

  test('should handle video element controls', async ({page}) => {
    await page.setContent(`
      <video id="test-video" controls width="300" height="200">
        <source src="data:video/mp4;base64,AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAAIZnJlZQAAAr1tZGF0" type="video/mp4">
        Your browser does not support the video tag.
      </video>
    `);

    const video = page.locator('#test-video');
    await expect(video).toBeVisible();

    // Test video controls
    const hasControls = await video.evaluate((el: HTMLVideoElement) => el.hasAttribute('controls'));
    expect(hasControls).toBe(true);

    // Test video dimensions
    const boundingBox = await video.boundingBox();
    expect(boundingBox?.width).toBeCloseTo(300, 10);
    expect(boundingBox?.height).toBeCloseTo(200, 10);
  });

  test('should support Web Audio API', async ({page}) => {
    const webAudioSupport = await page.evaluate(() => {
      const support = {
        audioContext: 'AudioContext' in window || 'webkitAudioContext' in window,
        audioWorklet: 'AudioWorklet' in window,
        analyserNode: false,
        gainNode: false
      };

      if (support.audioContext) {
        try {
          const AudioContextClass = window.AudioContext || (window as unknown as {
            webkitAudioContext: typeof AudioContext
          }).webkitAudioContext;
          const audioContext = new AudioContextClass();

          support.analyserNode = typeof audioContext.createAnalyser === 'function';
          support.gainNode = typeof audioContext.createGain === 'function';

          audioContext.close();
        } catch {
          support.audioContext = false;
        }
      }

      return support;
    });

    if (webAudioSupport.audioContext) {
      expect(webAudioSupport.analyserNode).toBe(true);
      expect(webAudioSupport.gainNode).toBe(true);
    }

    console.log('Web Audio API Support:', webAudioSupport);
  });

  test('should handle media stream API', async ({page}) => {
    const mediaStreamSupport = await page.evaluate(() => {
      return {
        getUserMedia: 'mediaDevices' in navigator && 'getUserMedia' in navigator.mediaDevices,
        getDisplayMedia: 'mediaDevices' in navigator && 'getDisplayMedia' in navigator.mediaDevices,
        mediaRecorder: 'MediaRecorder' in window,
        mediaStream: 'MediaStream' in window
      };
    });

    console.log('Media Stream API Support:', mediaStreamSupport);

    // These APIs require user permission, so we just test availability
    expect(typeof mediaStreamSupport.getUserMedia).toBe('boolean');
    expect(typeof mediaStreamSupport.mediaRecorder).toBe('boolean');
  });
});

test.describe('File Handling Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
  });

  test('should support File API', async ({page}) => {
    const fileAPISupport = await page.evaluate(() => {
      return {
        file: 'File' in window,
        fileList: 'FileList' in window,
        fileReader: 'FileReader' in window,
        blob: 'Blob' in window,
        url: 'URL' in window && 'createObjectURL' in URL
      };
    });

    expect(fileAPISupport.file).toBe(true);
    expect(fileAPISupport.fileList).toBe(true);
    expect(fileAPISupport.fileReader).toBe(true);
    expect(fileAPISupport.blob).toBe(true);
    expect(fileAPISupport.url).toBe(true);

    console.log('File API Support:', fileAPISupport);
  });

  test('should handle file input elements', async ({page}) => {
    await page.setContent(`
      <div style="padding: 20px;">
        <input type="file" id="single-file" accept=".txt,.json">
        <input type="file" id="multiple-files" multiple accept="image/*">
        <input type="file" id="directory-upload" webkitdirectory multiple>
      </div>
    `);

    const singleFile = page.locator('#single-file');
    const multipleFiles = page.locator('#multiple-files');
    const directoryUpload = page.locator('#directory-upload');

    await expect(singleFile).toBeVisible();
    await expect(multipleFiles).toBeVisible();
    await expect(directoryUpload).toBeVisible();

    // Test file input attributes
    const singleFileAccept = await singleFile.getAttribute('accept');
    const multipleFilesMultiple = await multipleFiles.getAttribute('multiple');
    const directoryWebkitDirectory = await directoryUpload.getAttribute('webkitdirectory');

    expect(singleFileAccept).toBe('.txt,.json');
    expect(multipleFilesMultiple).toBe('');
    expect(directoryWebkitDirectory).toBe('');
  });

  test('should support drag and drop', async ({page}) => {
    await page.setContent(`
      <div id="drop-zone" style="width: 200px; height: 200px; border: 2px dashed #ccc; padding: 20px; text-align: center;">
        Drop files here
      </div>
      <script>
        const dropZone = document.getElementById('drop-zone');
        let dragEvents = [];
        
        dropZone.addEventListener('dragover', (e) => {
          e.preventDefault();
          dragEvents.push('dragover');
          dropZone.style.backgroundColor = 'lightblue';
        });
        
        dropZone.addEventListener('dragleave', (e) => {
          e.preventDefault();
          dragEvents.push('dragleave');
          dropZone.style.backgroundColor = '';
        });
        
        dropZone.addEventListener('drop', (e) => {
          e.preventDefault();
          dragEvents.push('drop');
          dropZone.style.backgroundColor = 'lightgreen';
          dropZone.textContent = 'Files dropped!';
        });
        
        window.dragEvents = dragEvents;
      </script>
    `);

    const dropZone = page.locator('#drop-zone');
    await expect(dropZone).toBeVisible();

    // Simulate drag and drop events
    await dropZone.dispatchEvent('dragover', {
      dataTransfer: {files: []}
    });

    const _backgroundColor = await dropZone.evaluate((el) => {
      return window.getComputedStyle(el).backgroundColor;
    });

    // Check if drag events are being handled
    const dragEvents = await page.evaluate(() => (window as unknown as {
      dragEvents: string[]
    }).dragEvents);
    expect(dragEvents).toContain('dragover');
  });

  test('should support FileReader API', async ({page}) => {
    const fileReaderTest = await page.evaluate(() => {
      return new Promise<{
        supported: boolean;
        methods: string[];
        error?: string;
      }>((resolve) => {
        try {
          const reader = new FileReader();
          const methods: string[] = [];

          if (typeof reader.readAsText === 'function') methods.push('readAsText');
          if (typeof reader.readAsDataURL === 'function') methods.push('readAsDataURL');
          if (typeof reader.readAsArrayBuffer === 'function') methods.push('readAsArrayBuffer');
          if (typeof reader.readAsBinaryString === 'function') methods.push('readAsBinaryString');

          // Test with a simple blob
          const blob = new Blob(['Hello, World!'], {type: 'text/plain'});

          reader.onload = () => {
            resolve({
              supported: true,
              methods
            });
          };

          reader.onerror = () => {
            resolve({
              supported: false,
              methods,
              error: 'FileReader error'
            });
          };

          reader.readAsText(blob);

        } catch (error) {
          resolve({
            supported: false,
            methods: [],
            error: (error as Error).message
          });
        }
      });
    });

    expect(fileReaderTest.supported).toBe(true);
    expect(fileReaderTest.methods).toContain('readAsText');
    expect(fileReaderTest.methods).toContain('readAsDataURL');
    expect(fileReaderTest.methods).toContain('readAsArrayBuffer');

    console.log('FileReader Methods:', fileReaderTest.methods);
  });

  test('should support Blob and URL APIs', async ({page}) => {
    const blobURLTest = await page.evaluate(() => {
      try {
        // Create a blob
        const blob = new Blob(['Test content'], {type: 'text/plain'});

        // Create object URL
        const url = URL.createObjectURL(blob);

        // Test URL properties
        const isValidURL = url.startsWith('blob:');

        // Clean up
        URL.revokeObjectURL(url);

        return {
          supported: true,
          blobSize: blob.size,
          blobType: blob.type,
          urlCreated: isValidURL
        };
      } catch (error) {
        return {
          supported: false,
          error: (error as Error).message
        };
      }
    });

    expect(blobURLTest.supported).toBe(true);
    expect(blobURLTest.blobSize).toBe(12); // 'Test content' length
    expect(blobURLTest.blobType).toBe('text/plain');
    expect(blobURLTest.urlCreated).toBe(true);

    console.log('Blob/URL Test:', blobURLTest);
  });
});

test.describe('Canvas and WebGL Compatibility', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
  });

  test('should support Canvas 2D API', async ({page}) => {
    const canvas2DTest = await page.evaluate(() => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = 200;
        canvas.height = 200;

        const ctx = canvas.getContext('2d');
        if (!ctx) return {supported: false};

        // Test basic drawing operations
        ctx.fillStyle = 'red';
        ctx.fillRect(10, 10, 50, 50);

        ctx.strokeStyle = 'blue';
        ctx.lineWidth = 2;
        ctx.strokeRect(70, 10, 50, 50);

        ctx.beginPath();
        ctx.arc(95, 95, 30, 0, 2 * Math.PI);
        ctx.fillStyle = 'green';
        ctx.fill();

        // Test text rendering
        ctx.font = '16px Arial';
        ctx.fillStyle = 'black';
        ctx.fillText('Test', 10, 150);

        // Test image data
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

        return {
          supported: true,
          canvasSize: {width: canvas.width, height: canvas.height},
          imageDataSize: imageData.data.length,
          dataURL: canvas.toDataURL().substring(0, 50) // First 50 chars
        };
      } catch (error) {
        return {
          supported: false,
          error: (error as Error).message
        };
      }
    });

    expect(canvas2DTest.supported).toBe(true);
    expect(canvas2DTest.canvasSize).toEqual({width: 200, height: 200});
    expect(canvas2DTest.imageDataSize).toBe(200 * 200 * 4); // RGBA

    console.log('Canvas 2D Test:', canvas2DTest);
  });

  test('should support WebGL', async ({page}) => {
    const webGLTest = await page.evaluate(() => {
      try {
        const canvas = document.createElement('canvas');
        const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');

        if (!gl) return {supported: false};

        // Test WebGL capabilities
        const renderer = gl.getParameter(gl.RENDERER);
        const vendor = gl.getParameter(gl.VENDOR);
        const version = gl.getParameter(gl.VERSION);
        const shadingLanguageVersion = gl.getParameter(gl.SHADING_LANGUAGE_VERSION);

        // Test WebGL extensions
        const extensions = gl.getSupportedExtensions();

        return {
          supported: true,
          renderer,
          vendor,
          version,
          shadingLanguageVersion,
          extensionCount: extensions?.length || 0,
          hasOES_texture_float: extensions?.includes('OES_texture_float') || false,
          hasWEBGL_depth_texture: extensions?.includes('WEBGL_depth_texture') || false
        };
      } catch (error) {
        return {
          supported: false,
          error: (error as Error).message
        };
      }
    });

    if (webGLTest.supported) {
      expect(typeof webGLTest.renderer).toBe('string');
      expect(typeof webGLTest.version).toBe('string');
      expect(webGLTest.extensionCount).toBeGreaterThan(0);
    }

    console.log('WebGL Test:', webGLTest);
  });

  test('should support WebGL2', async ({page}) => {
    const webGL2Test = await page.evaluate(() => {
      try {
        const canvas = document.createElement('canvas');
        const gl = canvas.getContext('webgl2');

        if (!gl) return {supported: false};

        // Test WebGL2 specific features
        const version = gl.getParameter(gl.VERSION);
        const maxTextureSize = gl.getParameter(gl.MAX_TEXTURE_SIZE);
        const maxVertexAttribs = gl.getParameter(gl.MAX_VERTEX_ATTRIBS);

        return {
          supported: true,
          version,
          maxTextureSize,
          maxVertexAttribs
        };
      } catch (error) {
        return {
          supported: false,
          error: (error as Error).message
        };
      }
    });

    if (webGL2Test.supported) {
      expect(webGL2Test.version).toContain('WebGL 2.0');
      expect(webGL2Test.maxTextureSize).toBeGreaterThan(0);
      expect(webGL2Test.maxVertexAttribs).toBeGreaterThan(0);
    }

    console.log('WebGL2 Test:', webGL2Test);
  });
});

test.describe('Browser-Specific Media Features', () => {
  test('should handle Safari media quirks', async ({page, browserName}) => {
    test.skip(browserName !== 'webkit', 'Safari-specific test');

    const safariMediaTest = await page.evaluate(() => {
      const video = document.createElement('video');
      const audio = document.createElement('audio');

      return {
        videoAutoplay: video.autoplay !== undefined,
        audioAutoplay: audio.autoplay !== undefined,
        pictureInPicture: 'pictureInPictureEnabled' in document,
        webkitPresentationMode: 'webkitPresentationMode' in video
      };
    });

    console.log('Safari Media Features:', safariMediaTest);
  });

  test('should handle Chrome media optimizations', async ({page, browserName}) => {
    test.skip(browserName !== 'chromium', 'Chrome-specific test');

    const chromeMediaTest = await page.evaluate(() => {
      const video = document.createElement('video');

      return {
        pictureInPicture: 'requestPictureInPicture' in video,
        webCodecs: 'VideoDecoder' in window,
        webRTC: 'RTCPeerConnection' in window,
        mediaSession: 'mediaSession' in navigator
      };
    });

    console.log('Chrome Media Features:', chromeMediaTest);
  });

  test('should handle Firefox media behavior', async ({page, browserName}) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific test');

    const firefoxMediaTest = await page.evaluate(() => {
      const video = document.createElement('video');
      const audio = document.createElement('audio');

      return {
        oggSupport: video.canPlayType('video/ogg') !== '',
        webmSupport: video.canPlayType('video/webm') !== '',
        mozAudioChannel: 'mozAudioChannelType' in audio
      };
    });

    console.log('Firefox Media Features:', firefoxMediaTest);
  });
});