export const getAttendanceLocation = () => new Promise((resolve, reject) => {
  if (!window.isSecureContext) {
    reject(new Error('Chấm công bằng vị trí chỉ hoạt động trên localhost hoặc HTTPS.'));
    return;
  }
  if (!navigator.geolocation) {
    reject(new Error('Trình duyệt không hỗ trợ lấy vị trí.'));
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      resolve({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracyMeters: position.coords.accuracy,
      });
    },
    (error) => {
      if (error.code === error.PERMISSION_DENIED) {
        reject(new Error('Bạn cần cấp quyền vị trí để chấm công.'));
      } else if (error.code === error.TIMEOUT) {
        reject(new Error('Không lấy được vị trí, vui lòng thử lại.'));
      } else {
        reject(new Error('Không lấy được vị trí hiện tại.'));
      }
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
    }
  );
});
