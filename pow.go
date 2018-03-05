package main

import "fmt"
import "bufio"
import "os"
import "github.com/iotaledger/giota"
import "time"
import "strconv"

func main() {
	scanner := bufio.NewScanner(os.Stdin)

	scanner.Scan()
	giota.PowProcs, _ = strconv.Atoi(scanner.Text());

	scanner.Scan()
	powId, _ := strconv.Atoi(scanner.Text());

	powName, pow := giota.GetBestPoW()

	if powId == 0 {
		fmt.Println(powName)
	}

	if powId == 1 {
		pow = giota.PowGo
	}

	if powId == 2 {
		pow = giota.PowC
	}

	if powId == 3 {
		pow = giota.PowSSE
	}

	for true {
		scanner.Scan()
		input := scanner.Text()
		trytes, _ := giota.ToTrytes(input)
		tx, _ := giota.NewTransaction(trytes)

		const maxTimestampTrytes = "MMMMMMMMM"
		const TimestampTrinarySize = 27

		timestamp := giota.Int2Trits(time.Now().UnixNano()/1000000, TimestampTrinarySize).Trytes()
		tx.AttachmentTimestamp = timestamp
		tx.AttachmentTimestampLowerBound = ""
		tx.AttachmentTimestampUpperBound = maxTimestampTrytes

		tx.Nonce, _ = pow(tx.Trytes(), int(14))

		fmt.Println(tx.Trytes())
	}
}