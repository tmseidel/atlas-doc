// Build status polling
// Polls /api/build/status every 3 seconds and shows/hides the rebuilding banner

(function () {
    'use strict';

    var banner = document.getElementById('rebuilding-banner');
    if (!banner) return;

    var iframe = document.getElementById('docs-iframe');
    var overlay = document.getElementById('rebuilding-overlay');
    var needsReload = false;

    function setRebuilding(isRebuilding) {
        banner.classList.toggle('d-none', !isRebuilding);
        if (overlay) {
            overlay.classList.toggle('d-none', !isRebuilding);
            overlay.classList.toggle('d-flex', isRebuilding);
        }
    }

    function checkBuildStatus() {
        fetch('/api/build/status')
            .then(function (r) { return r.json(); })
            .then(function (status) {
                if (status === 'RUNNING') {
                    setRebuilding(true);
                    needsReload = true;
                } else {
                    if (needsReload && iframe) {
                        // Build just finished - reload iframe
                        iframe.src = iframe.src;
                        needsReload = false;
                    }
                    setRebuilding(false);
                }
            })
            .catch(function () {
                // Silently fail - polling will retry
            });
    }

    // Poll every 3 seconds
    setInterval(checkBuildStatus, 3000);

    // Also check on page load
    checkBuildStatus();
})();
