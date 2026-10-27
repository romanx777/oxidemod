#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cstdint>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <dirent.h>
#include <fstream>

static bool rd(pid_t pid, uintptr_t a, void* b, size_t n) {
    struct iovec l{ b, n }, r{ (void*)a, n };
    return process_vm_readv(pid, &l, 1, &r, 1, 0) == (ssize_t)n;
}
static bool wr(pid_t pid, uintptr_t a, const void* b, size_t n) {
    struct iovec l{ (void*)b, n }, r{ (void*)a, n };
    return process_vm_writev(pid, &l, 1, &r, 1, 0) == (ssize_t)n;
}
static std::string hex(const uint8_t* p, size_t n) {
    static const char* h = "0123456789abcdef";
    std::string s(n * 2, '0');
    for (size_t i = 0; i < n; ++i) { s[i*2] = h[p[i]>>4]; s[i*2+1] = h[p[i]&15]; }
    return s;
}
static int nib(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}
static bool unhex(const char* s, std::vector<uint8_t>& o) {
    size_t n = strlen(s); if (n % 2) return false; o.resize(n / 2);
    for (size_t i = 0; i < o.size(); ++i) {
        int a = nib(s[i*2]), b = nib(s[i*2+1]); if (a < 0 || b < 0) return false;
        o[i] = (uint8_t)((a << 4) | b);
    }
    return true;
}

int main() {
    setvbuf(stdout, nullptr, _IONBF, 0);
    char line[8192];
    while (fgets(line, sizeof(line), stdin)) {
        size_t L = strlen(line);
        while (L && (line[L-1] == '\n' || line[L-1] == '\r')) line[--L] = 0;
        if (!L) continue;
        std::vector<char*> t;
        for (char* p = strtok(line, " "); p; p = strtok(nullptr, " ")) t.push_back(p);
        if (t.empty()) continue;
        const char* cmd = t[0];

        if (!strcmp(cmd, "r") && t.size() == 4) {
            pid_t pid = atoi(t[1]);
            uintptr_t a = strtoull(t[2], nullptr, 16);
            size_t n = strtoul(t[3], nullptr, 10);
            if (!n || n > 4096) { printf("ERR size\n"); continue; }
            std::vector<uint8_t> b(n);
            if (!rd(pid, a, b.data(), n)) { printf("ERR read\n"); continue; }
            printf("OK %s\n", hex(b.data(), n).c_str());
        } else if (!strcmp(cmd, "w") && t.size() == 4) {
            pid_t pid = atoi(t[1]);
            uintptr_t a = strtoull(t[2], nullptr, 16);
            std::vector<uint8_t> b;
            if (!unhex(t[3], b)) { printf("ERR hex\n"); continue; }
            if (!wr(pid, a, b.data(), b.size())) { printf("ERR write\n"); continue; }
            printf("OK\n");
        } else if (!strcmp(cmd, "pid") && t.size() == 2) {
            std::string pkg = t[1];
            DIR* dir = opendir("/proc");
            if (!dir) { printf("ERR dir\n"); continue; }
            struct dirent* ent;
            bool found = false;
            while ((ent = readdir(dir))) {
                if (ent->d_type != DT_DIR) continue;
                if (atoi(ent->d_name) <= 0) continue;
                std::string path = "/proc/" + std::string(ent->d_name) + "/cmdline";
                std::ifstream f(path);
                std::string cmdl;
                if (std::getline(f, cmdl) && cmdl.find(pkg) != std::string::npos) {
                    printf("OK %s\n", ent->d_name); found = true; break;
                }
            }
            closedir(dir);
            if (!found) printf("ERR notfound\n");
        } else if (!strcmp(cmd, "base") && t.size() == 3) {
            pid_t pid = atoi(t[1]);
            std::string mod = t[2];
            std::string path = "/proc/" + std::to_string(pid) + "/maps";
            std::ifstream f(path);
            std::string line;
            bool found = false;
            while (std::getline(f, line)) {
                if (line.find(mod) != std::string::npos && line.find("r-xp") != std::string::npos) {
                    size_t dash = line.find('-');
                    printf("OK %s\n", line.substr(0, dash).c_str()); found = true; break;
                }
            }
            if (!found) printf("ERR nobase\n");
        } else printf("ERR cmd\n");
    }
    return 0;
}
